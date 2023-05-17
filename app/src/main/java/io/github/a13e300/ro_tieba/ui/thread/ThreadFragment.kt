package io.github.a13e300.ro_tieba.ui.thread

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.databinding.FragmentThreadBinding
import kotlinx.coroutines.launch

class ThreadFragment : Fragment() {

    companion object {
        fun newInstance() = ThreadFragment()
    }

    private val viewModel: ThreadViewModel by viewModels()
    private val args: ThreadFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentThreadBinding.inflate(inflater, container, false)
        if (viewModel.threadConfig.value == null) {
            viewModel.threadConfig.value = ThreadConfig(args.tid.toLong())
            Logger.d("update thread config")
        }
        val postAdapter = PostAdapter(PostComparator)
        binding.list.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = postAdapter
        }
        viewModel.threadTitle.observe(viewLifecycleOwner) {
            binding.toolbar.title = it
        }
        binding.toolbar.addMenuProvider(object : MenuProvider {
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.refresh) {
                    binding.list.scrollToPosition(0)
                    postAdapter.refresh()
                    return true
                }
                return false
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.thread_menu, menu)
            }
        })
        lifecycleScope.launch {
            viewModel.flow.collect { data ->
                postAdapter.submitData(data)
            }
        }
        return binding.root
    }

}