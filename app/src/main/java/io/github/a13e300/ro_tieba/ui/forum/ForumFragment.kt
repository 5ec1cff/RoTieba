package io.github.a13e300.ro_tieba.ui.forum

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isGone
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.panpf.sketch.displayImage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.BaseFragment
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.appendSimpleContent
import io.github.a13e300.ro_tieba.databinding.FragmentForumBinding
import io.github.a13e300.ro_tieba.databinding.FragmentForumThreadItemBinding
import io.github.a13e300.ro_tieba.misc.IconSpan
import io.github.a13e300.ro_tieba.misc.RoundSpan
import io.github.a13e300.ro_tieba.models.Content
import io.github.a13e300.ro_tieba.models.TiebaThread
import io.github.a13e300.ro_tieba.toSimpleString
import io.github.a13e300.ro_tieba.ui.DetailDialogFragment
import io.github.a13e300.ro_tieba.ui.photo.Photo
import io.github.a13e300.ro_tieba.ui.photo.PhotoViewModel
import io.github.a13e300.ro_tieba.ui.toDetail
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

class ForumFragment : BaseFragment() {

    private val viewModel: ForumViewModel by viewModels()
    private val args: ForumFragmentArgs by navArgs()
    private val photoViewModel: PhotoViewModel by viewModels({ findNavController().currentBackStackEntry!! })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentForumBinding.inflate(inflater, container, false)
        viewModel.forumName = args.fname
        viewModel.forumInfo.observe(viewLifecycleOwner) {
            // binding.toolbar.title = it.name
            binding.forumName.text = it.name
            binding.forumDesc.text = it.desc
            binding.forumAvatar.displayImage(it.avatarUrl)
        }
        setupToolbar(binding.toolbar)
        binding.appBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            binding.toolbarLayout.title = if (abs(verticalOffset) >= appBarLayout.totalScrollRange)
                viewModel.forumInfo.value?.name else null
        }
        binding.toolbar.setOnClickListener {
            binding.threadList.scrollToPosition(0)
        }
        val threadAdapter = ThreadAdapter(ThreadComparator)
        threadAdapter.addLoadStateListener { state ->
            (state.refresh as? LoadState.Error)?.error?.let {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.error_dialog_title)
                    .setMessage(it.message)
                    .setOnDismissListener {
                        navigateUp()
                    }
                    .show()
            }
        }
        binding.threadList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = threadAdapter
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val currentUid = App.instance.accountManager.currentAccount.first().uid
                if (viewModel.currentUid == null)
                    viewModel.currentUid = currentUid
                else if (currentUid != viewModel.currentUid) {
                    navigateUp()
                    return@repeatOnLifecycle
                }
                viewModel.flow.collect {
                    threadAdapter.submitData(it)
                }
            }
        }
        return binding.root
    }

    inner class ThreadAdapter(diffCallback: DiffUtil.ItemCallback<TiebaThread>) :
        PagingDataAdapter<TiebaThread, ThreadAdapter.ThreadViewHolder>(
            diffCallback
        ) {
        inner class ThreadViewHolder(val binding: FragmentForumThreadItemBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onBindViewHolder(holder: ThreadViewHolder, position: Int) {
            val thread = getItem(position) ?: return
            holder.binding.threadTitle.text = SpannableStringBuilder().apply {
                if (thread.isGood) {
                    val context = requireContext()
                    append(
                        "[精]",
                        RoundSpan(
                            context,
                            context.getColor(R.color.good_span_background),
                            context.getColor(R.color.good_span_text),
                            showText = "精"
                        ),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    append(" ")
                }
                append(thread.title)
            }
            holder.binding.threadTitle.isGone = holder.binding.threadTitle.text.isEmpty()
            holder.binding.threadContent.text = SpannableStringBuilder()
                .appendSimpleContent(thread.content, requireContext())
            holder.binding.threadUserName.text = thread.author.nick.ifEmpty { thread.author.name }
            holder.binding.threadInfo.setOnClickListener {
                val (ks, vs) = thread.toDetail()
                DetailDialogFragment.newInstance(ks, vs).show(childFragmentManager, "detail")
            }
            holder.binding.threadInfo.text = SpannableStringBuilder().apply {
                append(
                    "最后回复 ",
                    IconSpan(
                        AppCompatResources.getDrawable(
                            requireContext(),
                            R.drawable.ic_time
                        )!!
                    ),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                append(thread.time.toSimpleString())
                append(" ")
                append(
                    "回复数 ",
                    IconSpan(
                        AppCompatResources.getDrawable(
                            requireContext(),
                            R.drawable.ic_comment
                        )!!
                    ),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                append(thread.replyNum.toString())
                append(" ")
                append(
                    "观看数 ",
                    IconSpan(
                        AppCompatResources.getDrawable(
                            requireContext(),
                            R.drawable.ic_see
                        )!!
                    ),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                append(thread.viewNum.toString())
                append(" ")
                append(
                    "点赞 ",
                    IconSpan(
                        AppCompatResources.getDrawable(
                            requireContext(),
                            R.drawable.ic_agree
                        )!!
                    ),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                append(thread.agreeNum.toString())
                append(" ")
                append(
                    "点踩 ",
                    IconSpan(
                        AppCompatResources.getDrawable(
                            requireContext(),
                            R.drawable.ic_disagree
                        )!!
                    ),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                append(thread.disagreeNum.toString())
            }
            holder.binding.threadAvatar.displayImage(thread.author.avatarUrl)
            holder.binding.root.setOnClickListener {
                findNavController().navigate(MobileNavigationDirections.goToThread(thread.tid))
            }
            val images = thread.content.filterIsInstance<Content.ImageContent>()
            val image1 = images.firstOrNull()
            val image2 = images.getOrNull(1)
            val image3 = images.getOrNull(2)
            if (image1 != null) {
                holder.binding.previewImage1.visibility = View.VISIBLE
                holder.binding.previewImage1.displayImage(image1.previewSrc)
                holder.binding.previewImage1.setOnClickListener {
                    photoViewModel.currentIndex.value = 0
                    photoViewModel.photos = images.map {
                        Photo(
                            it.src, it.order, thread
                        )
                    }
                    findNavController().navigate(MobileNavigationDirections.viewPhotos())
                }
            } else {
                holder.binding.previewImage1.visibility = View.INVISIBLE
                holder.binding.previewImage1.setOnClickListener(null)
            }
            if (image2 != null) {
                holder.binding.previewImage2.visibility = View.VISIBLE
                holder.binding.previewImage2.displayImage(image2.previewSrc)
                holder.binding.previewImage2.setOnClickListener {
                    photoViewModel.currentIndex.value = 1
                    photoViewModel.photos = images.map {
                        Photo(
                            it.src, it.order, thread
                        )
                    }
                    findNavController().navigate(MobileNavigationDirections.viewPhotos())
                }
            } else {
                holder.binding.previewImage2.visibility = View.INVISIBLE
                holder.binding.previewImage2.setOnClickListener(null)
            }
            if (image3 != null) {
                holder.binding.previewImage3.visibility = View.VISIBLE
                holder.binding.previewImage3.displayImage(image3.previewSrc)
                holder.binding.previewImage3.setOnClickListener {
                    photoViewModel.currentIndex.value = 2
                    photoViewModel.photos = images.map {
                        Photo(
                            it.src, it.order, thread
                        )
                    }
                    findNavController().navigate(MobileNavigationDirections.viewPhotos())
                }
            } else {
                holder.binding.previewImage3.visibility = View.INVISIBLE
                holder.binding.previewImage3.setOnClickListener(null)
            }
            if (images.isEmpty()) {
                holder.binding.previewImage1.visibility = View.GONE
                holder.binding.previewImage2.visibility = View.GONE
                holder.binding.previewImage3.visibility = View.GONE
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreadViewHolder {
            return ThreadViewHolder(
                FragmentForumThreadItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }
    }
}

object ThreadComparator : DiffUtil.ItemCallback<TiebaThread>() {
    override fun areItemsTheSame(oldItem: TiebaThread, newItem: TiebaThread): Boolean {
        return oldItem.tid == newItem.tid
    }

    override fun areContentsTheSame(oldItem: TiebaThread, newItem: TiebaThread): Boolean {
        return oldItem == newItem
    }
}