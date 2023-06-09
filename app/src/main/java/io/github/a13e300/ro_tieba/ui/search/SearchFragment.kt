package io.github.a13e300.ro_tieba.ui.search

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.search.SearchView
import com.google.android.material.tabs.TabLayoutMediator
import io.github.a13e300.ro_tieba.BaseFragment
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.databinding.FragmentSearchBinding
import io.github.a13e300.ro_tieba.databinding.SearchSuggestionItemBinding
import io.github.a13e300.ro_tieba.ui.thread.ThreadFragmentDirections
import io.github.a13e300.ro_tieba.view.MySearchView

class SearchFragment : BaseFragment() {

    private val viewModel: SearchViewModel by viewModels()

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
        binding = FragmentSearchBinding.inflate(inflater, container, false)
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
                return 2
            }

            override fun createFragment(position: Int): Fragment {
                if (position == 0)
                    return SearchResultFragment()
                else
                    return SearchPostFragment()
            }

        }
        binding.searchView.onBackPressedListener = MySearchView.OnBackPressedListener {
            if (viewModel.searched)
                binding.searchView.hide()
            else navigateUp()
        }
        val myAdapter = SearchSuggestionAdapter()
        TabLayoutMediator(binding.searchTabLayout, binding.searchViewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.search_tab_bar_title)
                else -> getString(R.string.search_tab_post_title)
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
                l.add(Operation.SearchForum(s))
                l.add(Operation.SearchPosts(s))
                viewModel.suggestions = l
            }
            myAdapter.notifyDataSetChanged()
        }
        binding.searchSuggestions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = myAdapter
        }
        return binding.root
    }

    private fun performSearch(t: String, tab: Int) {
        binding.searchBar.text = t
        viewModel.fetchForums(t)
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
                    holder.binding.title.text = "进帖：${op.tid}"
                    holder.binding.root.setOnClickListener {
                        findNavController().navigate(ThreadFragmentDirections.goToThread(op.tid))
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