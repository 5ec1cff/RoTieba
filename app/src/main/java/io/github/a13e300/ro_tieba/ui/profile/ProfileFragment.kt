package io.github.a13e300.ro_tieba.ui.profile

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.panpf.sketch.displayImage
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import io.github.a13e300.ro_tieba.BaseFragment
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.databinding.FragmentProfileBinding
import io.github.a13e300.ro_tieba.models.UserForum
import io.github.a13e300.ro_tieba.openUserAtOtherClient
import io.github.a13e300.ro_tieba.ui.photo.Photo
import io.github.a13e300.ro_tieba.ui.photo.PhotoViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

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
        binding.appBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            binding.toolbarLayout.title = if (abs(verticalOffset) >= appBarLayout.totalScrollRange)
                viewModel.user.value?.showName else null
        }
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
        viewModel.uid = args.uid
        viewModel.portrait = args.portrait
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
        }
        binding.profileViewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return 2
            }

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> ProfileThreadsFragment()
                    1 -> ProfileForumsFragment()
                    else -> throw IllegalArgumentException("unknown position")
                }
            }
        }
        TabLayoutMediator(binding.profileTabLayout, binding.profileViewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "帖子"
                1 -> "关注的吧"
                else -> null
            }
        }.attach()
        if (savedInstanceState == null) {
            lifecycleScope.launch {
                viewModel.requestUser(args.uid, args.portrait)
            }
            viewModel.uid = args.uid
        }
        return binding.root
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
