package io.github.a13e300.ro_tieba.ui.profile

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.panpf.sketch.displayImage
import com.google.android.material.snackbar.Snackbar
import io.github.a13e300.ro_tieba.BaseFragment
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.databinding.FragmentHomeBarItemBinding
import io.github.a13e300.ro_tieba.databinding.FragmentProfileBinding
import io.github.a13e300.ro_tieba.models.UserForum
import io.github.a13e300.ro_tieba.openUserAtOtherClient
import io.github.a13e300.ro_tieba.ui.photo.Photo
import io.github.a13e300.ro_tieba.ui.photo.PhotoViewModel
import io.github.a13e300.ro_tieba.utils.appendLevelSpan
import kotlinx.coroutines.launch

class ProfileFragment : BaseFragment() {
    private lateinit var binding: FragmentProfileBinding
    private val viewModel: ProfileViewModel by viewModels()
    private val args: ProfileFragmentArgs by navArgs()
    private val photoViewModel: PhotoViewModel by viewModels({ findNavController().currentBackStackEntry!! })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        setupToolbar(binding.toolbar)
        binding.toolbar.setOnMenuItemClickListener {
            val profile = viewModel.user.value ?: return@setOnMenuItemClickListener false
            when (it.itemId) {
                R.id.open_at_other_client -> {
                    if (!openUserAtOtherClient(profile, requireContext())) {
                        Snackbar.make(
                            binding.root,
                            getString(R.string.no_other_apps_tips),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    return@setOnMenuItemClickListener true
                }
            }
            false
        }
        val forumAdapter = FollowForumAdapter(UserForumComparator)
        viewModel.uid = args.uid
        viewModel.user.observe(viewLifecycleOwner) { profile ->
            binding.userName.text = SpannableStringBuilder()
                .append(profile.name, StyleSpan(Typeface.BOLD), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                .apply {
                    if (profile.nick.isNotEmpty() && profile.nick != profile.name) {
                        append("(")
                        append(profile.nick)
                        append(")")
                    }
                }
            binding.userAvatar.displayImage(profile.avatarUrl)
            binding.userAvatar.setOnClickListener {
                photoViewModel.currentIndex.value = 0
                photoViewModel.photos = listOf(Photo(profile.realAvatarUrl, 0, profile))
                findNavController().navigate(MobileNavigationDirections.viewPhotos())
            }
            binding.userStat.text =
                "粉丝 ${profile.fanNum} 关注 ${profile.followNum} 发帖 ${profile.threadNum}"
            binding.userDesc.text = profile.desc
            if (viewModel.uid == 0L) {
                viewModel.uid = profile.uid
                forumAdapter.refresh()
            }
        }
        binding.forumList.adapter = forumAdapter
        lifecycleScope.launch {
            viewModel.flow.collect {
                forumAdapter.submitData(it)
            }
        }
        if (savedInstanceState == null) {
            lifecycleScope.launch {
                viewModel.requestUser(args.uid, args.portrait)
            }
        }
        return binding.root
    }

    inner class FollowForumAdapter(
        diffCallback: DiffUtil.ItemCallback<UserForum>
    ) : PagingDataAdapter<UserForum, FollowForumAdapter.FollowForumViewHolder>(diffCallback) {
        inner class FollowForumViewHolder(val binding: FragmentHomeBarItemBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onBindViewHolder(holder: FollowForumViewHolder, position: Int) {
            val bar = getItem(position) ?: return
            holder.binding.barName.text = bar.name
            holder.binding.barLevel.text =
                SpannableStringBuilder().appendLevelSpan(requireContext(), bar.levelId)
            holder.binding.root.setOnClickListener {
                findNavController().navigate(MobileNavigationDirections.goToForum(bar.name))
            }
            holder.binding.forumAvatar.displayImage(bar.avatarUrl)
            ViewCompat.setTooltipText(holder.binding.root, bar.name)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowForumViewHolder {
            return FollowForumViewHolder(
                FragmentHomeBarItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }
}

object UserForumComparator : DiffUtil.ItemCallback<UserForum>() {
    override fun areItemsTheSame(
        oldItem: UserForum,
        newItem: UserForum
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: UserForum,
        newItem: UserForum
    ): Boolean {
        return oldItem == newItem
    }
}
