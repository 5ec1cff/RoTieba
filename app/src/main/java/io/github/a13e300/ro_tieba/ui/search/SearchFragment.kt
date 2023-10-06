package io.github.a13e300.ro_tieba.ui.search

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.os.Bundle
import android.text.InputFilter.LengthFilter
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.search.SearchView
import com.google.android.material.tabs.TabLayoutMediator
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.BaseFragment
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.arch.Event
import io.github.a13e300.ro_tieba.databinding.FragmentSearchBinding
import io.github.a13e300.ro_tieba.databinding.SearchSuggestionItemBinding
import io.github.a13e300.ro_tieba.datastore.SearchHistory
import io.github.a13e300.ro_tieba.misc.OnPreImeBackPressedListener
import io.github.a13e300.ro_tieba.models.PostId
import io.github.a13e300.ro_tieba.ui.thread.ThreadFragmentDirections
import io.github.a13e300.ro_tieba.utils.navigateToPost
import io.github.a13e300.ro_tieba.utils.parseThreadLink
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

val USER_REGEX = Regex("\\d+|tb\\.1\\..*")

@SuppressLint("NotifyDataSetChanged")
class SearchFragment : BaseFragment() {

    private val viewModel: SearchViewModel by viewModels()
    private val args by navArgs<SearchFragmentArgs>()

    private lateinit var binding: FragmentSearchBinding
    private var mClipboardContent: CharSequence? = null
    private var mSearchText: String = ""
    private val mSuggestions = mutableListOf<Operation>()
    private lateinit var mSuggestionAdapter: SearchSuggestionAdapter

    override fun onResume() {
        super.onResume()
        if (viewModel.needShowSearch) {
            binding.searchView.show()
            binding.searchView.requestFocusAndShowKeyboard()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (!viewModel.initialized) {
            args.forum?.let {
                viewModel.forum = it
            }
            viewModel.searchAtForum = args.forum != null
        }
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        val hint = if (viewModel.searchAtForum)
            getString(R.string.searchbar_search_at_forum_hint, viewModel.forum)
        else getString(R.string.searchbar_hint)
        binding.searchView.hint = hint
        binding.searchBar.hint = hint
        binding.searchView.editText.apply {
            filters = arrayOf(LengthFilter(100))
            maxLines = 1
        }
        binding.searchView.editText.setOnEditorActionListener { textView, i, keyEvent ->
            if (i != EditorInfo.IME_ACTION_SEARCH &&
                !(keyEvent?.action == KeyEvent.ACTION_DOWN
                        && keyEvent.keyCode in intArrayOf(
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER
                )
                        )
            )
                return@setOnEditorActionListener false
            val t = textView.text
            if (t.isNotEmpty())
                performSearch(t.toString(), -1)
            else
                binding.searchView.hide()
            true
        }
        binding.searchView.addTransitionListener { _, _, newState ->
            if (newState == SearchView.TransitionState.HIDDEN) viewModel.needShowSearch = false
            else if (newState == SearchView.TransitionState.SHOWN) {
                viewModel.needShowSearch = true
                if (!viewModel.searchAtForum) {
                    mClipboardContent =
                        requireContext().getSystemService(ClipboardManager::class.java).primaryClip?.getItemAt(
                            0
                        )?.text
                }
                updateOperations()
            }
        }
        binding.searchViewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return if (viewModel.searchAtForum) 1 else 3
            }

            override fun createFragment(position: Int): Fragment {
                return if (position == 0)
                    if (viewModel.searchAtForum)
                        SearchPostFragment()
                    else
                        SearchForumFragment()
                else if (position == 1)
                    SearchPostFragment()
                else
                    SearchUserFragment()
            }

        }
        binding.searchView.onBackPressedListener = OnPreImeBackPressedListener {
            binding.searchView.clearFocusAndHideKeyboard()
            if (viewModel.currentKeyword.isNotEmpty())
                binding.searchView.hide()
            else navigateUp()
            return@OnPreImeBackPressedListener true
        }
        mSuggestionAdapter = SearchSuggestionAdapter()
        binding.searchSuggestions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = mSuggestionAdapter
        }
        if (viewModel.searchAtForum) {
            binding.searchTabLayout.isGone = true
            binding.searchViewPager.isUserInputEnabled = false
        } else {
            binding.searchViewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    binding.searchTabLayout.background = AppCompatResources.getDrawable(
                        requireContext(), when (position) {
                            1 -> R.drawable.background
                            else -> R.drawable.background_with_divider
                        }
                    )
                }
            })
            TabLayoutMediator(binding.searchTabLayout, binding.searchViewPager) { tab, position ->
                tab.text = when (position) {
                    0 -> getString(R.string.search_tab_bar_title)
                    1 -> getString(R.string.search_tab_post_title)
                    else -> getString(R.string.search_tab_user_title)
                }
            }.attach()
            binding.searchView.editText.doAfterTextChanged { e ->
                mSearchText = e?.toString() ?: ""
                updateOperations()
            }
            viewModel.forumCount.observe(viewLifecycleOwner) {
                binding.searchTabLayout.getTabAt(0)?.text =
                    if (it == null) getString(R.string.search_tab_bar_title)
                    else getString(R.string.search_tab_bar_title) + "($it)"
            }
            viewModel.userCount.observe(viewLifecycleOwner) {
                binding.searchTabLayout.getTabAt(2)?.text =
                    if (it == null) getString(R.string.search_tab_user_title)
                    else getString(R.string.search_tab_user_title) + "($it)"
            }
        }
        lifecycleScope.launch {
            App.instance.searchHistoryDataStore.data.collectLatest {
                updateOperations()
            }
        }
        return binding.root
    }

    private fun updateOperations() {
        mSuggestions.clear()
        val l = mSuggestions
        val s = mSearchText
        if (!viewModel.searchAtForum) {
            if (s.isNotEmpty()) {
                l.add(Operation.GoToForum(s))
                s.toLongOrNull()?.also { l.add(Operation.GoToThread(PostId.Thread(it))) }
                s.parseThreadLink()?.also { l.add(Operation.GoToThread(it)) }
                if (s.matches(USER_REGEX)) {
                    l.add(Operation.GoToUser(s))
                }
                l.add(Operation.SearchForum(s))
                l.add(Operation.SearchPosts(s))
                l.add(Operation.SearchUsers(s))
            }
            mClipboardContent?.also { clip ->
                clip.toString().parseThreadLink()?.also {
                    l.add(Operation.GoToThread(it, true))
                }
            }
        }
        if (s.isEmpty()) {
            val histories =
                runBlocking { App.instance.searchHistoryDataStore.data.first().entriesList }
            l.addAll(histories.map { Operation.History(it) })
            if (histories.isNotEmpty()) l.add(Operation.RemoveHistories)
        }
        mSuggestionAdapter.notifyDataSetChanged()
    }

    private fun performSearch(t: String, tab: Int) {
        binding.searchBar.setText(t)
        viewModel.currentKeyword = t
        binding.searchView.hide()
        if (tab >= 0)
            binding.searchViewPager.currentItem = tab
        viewModel.searchForumEvent.value = Event(t)
        viewModel.searchUserEvent.value = Event(t)
        viewModel.searchPostEvent.value = Event(t)
        viewModel.forumCount.value = null
        viewModel.userCount.value = null
        lifecycleScope.launch {
            App.instance.historyManager.addSearch(
                SearchHistory.Entry.newBuilder().setKeyword(t).build()
            )
        }
    }

    private fun removeHistoryDialog(entry: SearchHistory.Entry) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("确定要删除吗？")
            .setMessage(entry.keyword)
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    App.instance.historyManager.removeSearch(entry)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun removeAllHistoriesDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("确定要删除所有历史记录吗？")
            .setPositiveButton("删除") { _, _ ->
                lifecycleScope.launch {
                    App.instance.historyManager.clearSearch()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    class SearchSuggestionViewHolder(val binding: SearchSuggestionItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class SearchSuggestionAdapter : RecyclerView.Adapter<SearchSuggestionViewHolder>() {
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: SearchSuggestionViewHolder, position: Int) {
            holder.binding.root.setOnClickListener(null)
            holder.binding.root.setOnLongClickListener(null)
            val op = mSuggestions[position]
            holder.binding.inputButton.isVisible =
                op is Operation.History && !viewModel.searchAtForum
            when (op) {
                is Operation.History -> {
                    val kw = op.entry.keyword
                    holder.binding.title.text = kw
                    holder.binding.root.setOnClickListener {
                        performSearch(kw, -1)
                    }
                    holder.binding.root.setOnLongClickListener {
                        removeHistoryDialog(op.entry)
                        true
                    }
                    holder.binding.inputButton.setOnClickListener {
                        binding.searchView.editText.setText(kw)
                    }
                }

                is Operation.RemoveHistories -> {
                    holder.binding.title.text = "清空历史记录"
                    holder.binding.root.setOnClickListener {
                        removeAllHistoriesDialog()
                    }
                }

                is Operation.GoToForum -> {
                    holder.binding.title.text = "进吧：${op.name}"
                    holder.binding.root.setOnClickListener {
                        findNavController().navigate(ThreadFragmentDirections.goToForum(op.name))
                    }
                }

                is Operation.GoToThread -> {
                    holder.binding.title.text =
                        if (op.fromClip) "打开剪切板的帖子：${op.id.tid}" else
                            "进帖：${op.id.tid}"
                    holder.binding.root.setOnClickListener {
                        findNavController().navigateToPost(op.id)
                    }
                }

                is Operation.SearchForum -> {
                    holder.binding.title.text = "搜吧：${op.name}"
                    holder.binding.root.setOnClickListener {
                        performSearch(op.name, 0)
                    }
                }

                is Operation.SearchPosts -> {
                    holder.binding.title.text = "搜帖：${op.keyword}"
                    holder.binding.root.setOnClickListener {
                        performSearch(op.keyword, 1)
                    }
                }

                is Operation.SearchUsers -> {
                    holder.binding.title.text = "搜人：${op.keyword}"
                    holder.binding.root.setOnClickListener {
                        performSearch(op.keyword, 2)
                    }
                }

                is Operation.GoToUser -> {
                    holder.binding.title.text = "查看用户：${op.uidOrPortrait}"
                    holder.binding.root.setOnClickListener {
                        findNavController().navigate(MobileNavigationDirections.showProfile(op.uidOrPortrait))
                    }
                }
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): SearchSuggestionViewHolder {
            return SearchSuggestionViewHolder(
                SearchSuggestionItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun getItemCount(): Int {
            return mSuggestions.size
        }
    }

}