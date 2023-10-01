package io.github.a13e300.ro_tieba.ui.profile

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.databinding.FragmentProfileFollowedForumsBinding
import io.github.a13e300.ro_tieba.databinding.FragmentProfileForumItemBinding
import io.github.a13e300.ro_tieba.misc.PauseLoadOnQuickScrollListener
import io.github.a13e300.ro_tieba.models.UserForum
import io.github.a13e300.ro_tieba.utils.appendLevelSpan
import io.github.a13e300.ro_tieba.utils.displayImageInList
import kotlinx.coroutines.launch

class ProfileForumsFragment : Fragment() {
    private val viewModel: ProfileViewModel by viewModels({ requireParentFragment() })
    private lateinit var binding: FragmentProfileFollowedForumsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileFollowedForumsBinding.inflate(inflater, container, false)
        val forumAdapter = FollowForumAdapter(UserForumComparator)
        binding.forumList.apply {
            adapter = forumAdapter
            layoutManager =
                StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            addOnScrollListener(PauseLoadOnQuickScrollListener())
        }
        forumAdapter.addLoadStateListener { state ->
            var showTips = false
            if (state.append is LoadState.NotLoading && state.append.endOfPaginationReached && forumAdapter.itemCount == 0) {
                showTips = true
                binding.resultTips.text = if (viewModel.followedForumsHidden)
                    getString(R.string.user_follow_forums_hidden)
                else getString(R.string.user_follow_forums_empty)
            }
            if (state.refresh is LoadState.Error) {
                showTips = true
                binding.resultTips.text = (state.refresh as LoadState.Error).error.message
            }
            binding.resultTips.isVisible = showTips
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.forumsFlow.collect {
                forumAdapter.submitData(it)
            }
        }
        return binding.root
    }


    inner class FollowForumAdapter(
        diffCallback: DiffUtil.ItemCallback<UserForum>
    ) : PagingDataAdapter<UserForum, FollowForumAdapter.FollowForumViewHolder>(diffCallback) {
        inner class FollowForumViewHolder(val binding: FragmentProfileForumItemBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onBindViewHolder(holder: FollowForumViewHolder, position: Int) {
            val bar = getItem(position) ?: return
            holder.binding.barName.text = bar.name
            holder.binding.barLevel.text =
                SpannableStringBuilder().appendLevelSpan(requireContext(), bar.levelId)
            holder.binding.root.setOnClickListener {
                findNavController().navigate(MobileNavigationDirections.goToForum(bar.name))
            }
            holder.binding.forumAvatar.displayImageInList(bar.avatarUrl)
            holder.binding.slogan.text = bar.desc
            ViewCompat.setTooltipText(holder.binding.root, bar.name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowForumViewHolder {
            return FollowForumViewHolder(
                FragmentProfileForumItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }
}