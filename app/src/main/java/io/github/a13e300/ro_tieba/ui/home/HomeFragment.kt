package io.github.a13e300.ro_tieba.ui.home

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.BaseFragment
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.account.AccountManager
import io.github.a13e300.ro_tieba.databinding.FragmentHomeBarItemBinding
import io.github.a13e300.ro_tieba.databinding.FragmentHomeBinding
import io.github.a13e300.ro_tieba.misc.PauseLoadOnQuickScrollListener
import io.github.a13e300.ro_tieba.models.UserForum
import io.github.a13e300.ro_tieba.ui.profile.UserForumComparator
import io.github.a13e300.ro_tieba.utils.appendLevelSpan
import io.github.a13e300.ro_tieba.utils.displayImageInList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeFragment : BaseFragment() {
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
                return when (menuItem.itemId) {
                    R.id.search -> {
                        findMainNavController()
                            .navigate(MobileNavigationDirections.homeSearch())
                        true
                    }

                    R.id.history -> {
                        findMainNavController()
                            .navigate(MobileNavigationDirections.showHistory())
                        true
                    }

                    else -> false
                }
            }
        })
        val forumAdapter = ForumAdapter(UserForumComparator)
        binding.barList.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = forumAdapter
            addOnScrollListener(PauseLoadOnQuickScrollListener())
        }
        binding.loginButton.setOnClickListener {
            findMainNavController().navigate(MobileNavigationDirections.manageAccounts())
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val currentUid = App.instance.accountManager.currentAccount.first().uid
                viewModel.updateUid(currentUid)
                binding.loginTipsScreen.isVisible = currentUid == AccountManager.ACCOUNT_ANONYMOUS
                viewModel.flow.collect {
                    forumAdapter.submitData(it)
                }
            }
        }
        forumAdapter.addLoadStateListener {
            val state = it.refresh
            when (state) {
                is LoadState.Error -> {
                    binding.tipsScreen.isVisible = true
                    binding.errorTips.text = state.error.message
                }

                else -> {
                    binding.tipsScreen.isVisible = false
                }
            }
        }
        binding.refreshButton.setOnClickListener {
            forumAdapter.refresh()
        }
        return binding.root
    }

    inner class ForumAdapter(
        diffCallback: DiffUtil.ItemCallback<UserForum>
    ) : PagingDataAdapter<UserForum, ForumAdapter.BarViewHolder>(diffCallback) {
        inner class BarViewHolder(val binding: FragmentHomeBarItemBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onBindViewHolder(holder: BarViewHolder, position: Int) {
            val bar = getItem(position) ?: return
            holder.binding.barName.text = bar.name
            holder.binding.barLevel.text =
                SpannableStringBuilder().appendLevelSpan(requireContext(), bar.levelId)
            holder.binding.root.setOnClickListener {
                findMainNavController().navigate(MobileNavigationDirections.goToForum(bar.name))
            }
            holder.binding.forumAvatar.displayImageInList(bar.avatarUrl)
            ViewCompat.setTooltipText(holder.binding.root, bar.name)
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
