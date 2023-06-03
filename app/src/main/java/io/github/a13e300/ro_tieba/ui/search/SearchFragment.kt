package io.github.a13e300.ro_tieba.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.a13e300.ro_tieba.databinding.FragmentSearchBinding
import io.github.a13e300.ro_tieba.databinding.SearchSuggestionItemBinding
import io.github.a13e300.ro_tieba.ui.thread.ThreadFragmentDirections
import io.github.a13e300.ro_tieba.view.MySearchView

class SearchFragment : Fragment() {

    private val viewModel: SearchViewModel by viewModels()

    private lateinit var binding: FragmentSearchBinding

    override fun onResume() {
        super.onResume()
        binding.searchView.show()
        binding.searchView.requestFocusAndShowKeyboard()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        binding.searchView.editText.setOnEditorActionListener { textView, i, keyEvent ->
            binding.searchBar.text = textView.text
            binding.searchView.hide()
            false
        }
        binding.searchView.onBackPressedListener = MySearchView.OnBackPressedListener {
            if (binding.searchView.text?.isEmpty() == true)
                findNavController().navigateUp()
        }
        val myAdapter = SearchSuggestionAdapter()
        binding.searchView.editText.doAfterTextChanged { e ->
            if (e.isNullOrEmpty()) {
                viewModel.suggestions = emptyList()
            } else {
                val l = mutableListOf<Operation>()
                val s = e.toString()
                l.add(Operation.GoToBar(s))
                s.toLongOrNull()?.also { l.add(Operation.GoToThread(it)) }
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

    class SearchSuggestionViewHolder(val binding: SearchSuggestionItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class SearchSuggestionAdapter : RecyclerView.Adapter<SearchSuggestionViewHolder>() {
        override fun onBindViewHolder(holder: SearchSuggestionViewHolder, position: Int) {
            val op = viewModel.suggestions[position]
            when (op) {
                is Operation.GoToBar -> {
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