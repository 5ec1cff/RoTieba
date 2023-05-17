package io.github.a13e300.ro_tieba.ui.home

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.api.json.GetFollowForums
import io.github.a13e300.ro_tieba.databinding.FragmentHomeBarItemBinding
import io.github.a13e300.ro_tieba.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    fun findMainNavController() =
        requireActivity().findNavController(R.id.nav_host_fragment_activity_main)

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.toolbar.title = getString(R.string.title_home)
        binding.toolbar.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.home_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.search) {
                    findMainNavController()
                        .navigate(R.id.navigation_search)
                    return true
                }
                return false
            }
        })
        val barAdapter = BarAdapter(BarComparator)
        binding.barList.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = barAdapter
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    App.instance.accountManager.currentAccount.collect {
                        if (it.uid != viewModel.uid.value) {
                            viewModel.uid.value = it.uid
                            barAdapter.refresh()
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewModel.flow.collect {
                barAdapter.submitData(it)
            }
        }
        return binding.root
    }

    inner class BarAdapter(
        diffCallback: DiffUtil.ItemCallback<GetFollowForums.Forum>
    ) : PagingDataAdapter<GetFollowForums.Forum, BarAdapter.BarViewHolder>(diffCallback) {
        inner class BarViewHolder(val binding: FragmentHomeBarItemBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onBindViewHolder(holder: BarViewHolder, position: Int) {
            val bar = getItem(position) ?: return
            holder.binding.barName.text = bar.name
            holder.binding.barLevel.text = bar.levelId
            holder.binding.root.setOnClickListener {
                findMainNavController().navigate(MobileNavigationDirections.goToForum(bar.name))
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarViewHolder {
            return BarViewHolder(
                FragmentHomeBarItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

object BarComparator : DiffUtil.ItemCallback<GetFollowForums.Forum>() {
    override fun areItemsTheSame(
        oldItem: GetFollowForums.Forum,
        newItem: GetFollowForums.Forum
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: GetFollowForums.Forum,
        newItem: GetFollowForums.Forum
    ): Boolean {
        return oldItem == newItem
    }
}