package io.github.a13e300.ro_tieba.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import io.github.a13e300.ro_tieba.databinding.FragmentSearchBinding
import io.github.a13e300.ro_tieba.ui.thread.ThreadFragmentDirections

class SearchFragment : Fragment() {

    companion object {
        fun newInstance() = SearchFragment()
    }

    private lateinit var viewModel: SearchViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(SearchViewModel::class.java)
        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSearchBinding.inflate(inflater, container, false)
        binding.searchView.editText.setOnEditorActionListener { textView, i, keyEvent ->
            binding.searchBar.setText(textView.text)
            binding.searchView.hide()
            textView.text?.also {
                findNavController().navigate(ThreadFragmentDirections.goToThread(it.toString()))
            }
            false
        }
        return binding.root
    }

}