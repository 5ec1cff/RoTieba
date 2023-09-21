package io.github.a13e300.ro_tieba.ui.search

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isGone
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.search.SearchView
import com.google.android.material.tabs.TabLayoutMediator
import io.github.a13e300.ro_tieba.BaseFragment
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.databinding.FragmentSearchBinding
import io.github.a13e300.ro_tieba.databinding.SearchSuggestionItemBinding
import io.github.a13e300.ro_tieba.misc.OnPreImeBackPressedListener
import io.github.a13e300.ro_tieba.ui.thread.ThreadFragmentDirections

val USER_REGEX = Regex("\\d+|tb\\.1\\..*")
val THREAD_REGEX = Regex("tieba\\.baidu\\.com/p/(\\d+)(.*[\\?&]pid=(\\d+))?")

class SearchFragment : BaseFragment() {

    private val viewModel: SearchViewModel by viewModels()
    private val args by navArgs<SearchFragmentArgs>()

    private lateinit var binding: FragmentSearchBinding

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
            performSearch(t.toString(), -1)
            true
        }
        binding.searchView.addTransitionListener { _, _, newState ->
            if (newState == SearchView.TransitionState.HIDDEN) viewModel.needShowSearch = false
            else if (newState == SearchView.TransitionState.SHOWN) viewModel.needShowSearch = true
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
            if (viewModel.forumSearched)
                binding.searchView.hide()
            else navigateUp()
            return@OnPreImeBackPressedListener true
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
            val myAdapter = SearchSuggestionAdapter()
            TabLayoutMediator(binding.searchTabLayout, binding.searchViewPager) { tab, position ->
                tab.text = when (position) {
                    0 -> getString(R.string.search_tab_bar_title)
                    1 -> getString(R.string.search_tab_post_title)
                    else -> getString(R.string.search_tab_user_title)
                }
            }.attach()
            binding.searchView.editText.doAfterTextChanged { e ->
                if (e.isNullOrEmpty()) {
                    viewModel.suggestions = emptyList()
                } else {
                    val l = mutableListOf<Operation>()
                    val s = e.toString()
                    l.add(Operation.GoToForum(s))
                    s.toLongOrNull()?.also { l.add(Operation.GoToThread(it)) }
                    THREAD_REGEX.find(s)?.let {
                        val tid = it.groupValues[1].toLongOrNull()
                        val pid = it.groupValues[3].toLongOrNull() ?: 0L
                        if (tid != null) {
                            l.add(Operation.GoToThread(tid, pid))
                        }
                    }
                    if (s.matches(USER_REGEX)) {
                        l.add(Operation.GoToUser(s))
                    }
                    l.add(Operation.SearchForum(s))
                    l.add(Operation.SearchPosts(s))
                    l.add(Operation.SearchUsers(s))
                    viewModel.suggestions = l
                }
                myAdapter.notifyDataSetChanged()
            }
            binding.searchSuggestions.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = myAdapter
            }
        }
        return binding.root
    }

    private fun performSearch(t: String, tab: Int) {
        binding.searchBar.text = t
        if (!viewModel.searchAtForum) {
            viewModel.fetchForums(t)
            viewModel.fetchUsers(t)
        }
        viewModel.currentKeyword.value = t
        binding.searchView.hide()
        if (tab >= 0)
            binding.searchViewPager.currentItem = tab
    }

    class SearchSuggestionViewHolder(val binding: SearchSuggestionItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class SearchSuggestionAdapter : RecyclerView.Adapter<SearchSuggestionViewHolder>() {
        override fun onBindViewHolder(holder: SearchSuggestionViewHolder, position: Int) {
            val op = viewModel.suggestions[position]
            when (op) {
                is Operation.GoToForum -> {
                    holder.binding.title.text = "进吧：${op.name}"
                    holder.binding.root.setOnClickListener {
                        findNavController().navigate(ThreadFragmentDirections.goToForum(op.name))
                    }
                }

                is Operation.GoToThread -> {
                    holder.binding.title.text =
                        "进帖：${op.tid}${if (op.pid != 0L) " / ${op.pid}" else ""}"
                    holder.binding.root.setOnClickListener {
                        findNavController().navigate(
                            ThreadFragmentDirections.goToThread(op.tid).setPid(op.pid)
                        )
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
            return viewModel.suggestions.size
        }
    }

}