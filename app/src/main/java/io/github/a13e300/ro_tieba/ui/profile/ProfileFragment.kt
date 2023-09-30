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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.BaseFragment
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.databinding.FragmentProfileBinding
import io.github.a13e300.ro_tieba.db.EntryType
import io.github.a13e300.ro_tieba.db.HistoryEntry
import io.github.a13e300.ro_tieba.models.Photo
import io.github.a13e300.ro_tieba.models.UserForum
import io.github.a13e300.ro_tieba.ui.photo.PhotoViewModel
import io.github.a13e300.ro_tieba.ui.photo.imageSource
import io.github.a13e300.ro_tieba.utils.copyText
import io.github.a13e300.ro_tieba.utils.openUserAtOtherClient
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
                viewModel.user.value?.getOrNull()?.showName else null
        }
        binding.toolbar.setOnMenuItemClickListener {
            val profile =
                viewModel.user.value?.getOrNull() ?: return@setOnMenuItemClickListener false
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

                R.id.copy_uid -> {
                    val uid = viewModel.user.value?.getOrNull()?.uid ?: viewModel.uid
                    copyText(uid.toString())
                    return@setOnMenuItemClickListener true
                }

                R.id.copy_portrait -> {
                    val portrait =
                        viewModel.user.value?.getOrNull()?.portrait ?: viewModel.portrait ?: ""
                    copyText(portrait)
                    return@setOnMenuItemClickListener true
                }
            }
            false
        }
        viewModel.user.observe(viewLifecycleOwner) { p ->
            p.fold({ profile ->
                binding.userName.text = SpannableStringBuilder()
                    .append(
                        profile.name,
                        StyleSpan(Typeface.BOLD),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
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
                    photoViewModel.photos =
                        listOf(Photo(profile.realAvatarUrl, 0, imageSource(profile)))
                    findNavController().navigate(MobileNavigationDirections.viewPhotos())
                }
                binding.userStat.text =
                    "粉丝 ${profile.fanNum} 关注 ${profile.followNum} 发帖 ${profile.threadNum}"
                binding.userDesc.text = profile.desc
                if (!viewModel.historyAdded) {
                    updateHistory()
                    viewModel.historyAdded = true
                }
            }, { err ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.error_dialog_title)
                    .setMessage(err.message)
                    .setOnDismissListener {
                        navigateUp()
                    }
                    .show()
            })
        }
        binding.profileViewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return 3
            }

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> ProfileThreadsFragment()
                    1 -> ProfileReplyFragment()
                    2 -> ProfileForumsFragment()
                    else -> throw IllegalArgumentException("unknown position")
                }
            }
        }
        TabLayoutMediator(binding.profileTabLayout, binding.profileViewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "帖子"
                1 -> "回复"
                2 -> "关注的吧"
                else -> null
            }
        }.attach()
        if (!viewModel.initialized) {
            val uid = args.uidOrPortrait.toLongOrNull()
            if (uid != null) {
                viewModel.uid = uid
                viewModel.portrait = null
            } else {
                viewModel.uid = 0L
                viewModel.portrait = args.uidOrPortrait
            }
            viewModel.initialized = true
            lifecycleScope.launch {
                viewModel.requestUser(viewModel.uid, viewModel.portrait)
            }
        }
        return binding.root
    }

    private fun updateHistory() {
        val user = viewModel.user.value?.getOrNull() ?: return
        lifecycleScope.launch {
            App.instance.historyManager.updateHistory(
                HistoryEntry(
                    type = EntryType.USER,
                    id = user.uid.toString(),
                    time = System.currentTimeMillis(),
                    userName = user.name,
                    userNick = user.nick,
                    userAvatar = user.avatarUrl
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
