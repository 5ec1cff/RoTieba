package io.github.a13e300.ro_tieba.ui.forum

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.panpf.sketch.displayImage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.BaseFragment
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.databinding.FragmentForumBinding
import io.github.a13e300.ro_tieba.databinding.FragmentForumThreadItemBinding
import io.github.a13e300.ro_tieba.db.EntryType
import io.github.a13e300.ro_tieba.db.HistoryEntry
import io.github.a13e300.ro_tieba.misc.IconSpan
import io.github.a13e300.ro_tieba.misc.PauseLoadOnQuickScrollListener
import io.github.a13e300.ro_tieba.misc.RoundSpan
import io.github.a13e300.ro_tieba.models.ForumSortType
import io.github.a13e300.ro_tieba.models.ForumTab
import io.github.a13e300.ro_tieba.models.Photo
import io.github.a13e300.ro_tieba.models.ThreadType
import io.github.a13e300.ro_tieba.ui.DetailDialogFragment
import io.github.a13e300.ro_tieba.ui.photo.PhotoViewModel
import io.github.a13e300.ro_tieba.ui.photo.toPhoto
import io.github.a13e300.ro_tieba.ui.toDetail
import io.github.a13e300.ro_tieba.utils.appendSimpleContent
import io.github.a13e300.ro_tieba.utils.configureDefaults
import io.github.a13e300.ro_tieba.utils.displayImageInList
import io.github.a13e300.ro_tieba.utils.openForumAtOtherClient
import io.github.a13e300.ro_tieba.utils.setSelectedData
import io.github.a13e300.ro_tieba.utils.toSimpleString
import kotlinx.coroutines.launch
import kotlin.math.abs

class ForumFragment : BaseFragment() {

    private val viewModel: ForumViewModel by viewModels()
    private val args: ForumFragmentArgs by navArgs()
    private val photoViewModel: PhotoViewModel by viewModels({ findNavController().currentBackStackEntry!! })
    private lateinit var binding: FragmentForumBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForumBinding.inflate(inflater, container, false)
        viewModel.forumName = args.fname
        binding.toolbar.setOnMenuItemClickListener {
            return@setOnMenuItemClickListener when (it.itemId) {
                R.id.search -> {
                    findNavController().navigate(
                        MobileNavigationDirections.homeSearch().setForum(viewModel.forumName)
                    )
                    true
                }

                R.id.open_at_other_client -> {
                    if (!openForumAtOtherClient(viewModel.forumName, requireContext())) {
                        Snackbar.make(
                            binding.root,
                            getString(R.string.no_other_apps_tips),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    true
                }

                else -> false
            }
        }
        setupToolbar(binding.toolbar)
        binding.appBar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            binding.toolbarLayout.title = if (abs(verticalOffset) >= appBarLayout.totalScrollRange)
                viewModel.forumInfo.value?.name else null
        }
        binding.toolbar.setOnClickListener {
            binding.threadList.scrollToPosition(0)
        }
        viewModel.forumInfo.observe(viewLifecycleOwner) {
            // binding.toolbar.title = it.name
            binding.forumName.text = it.name
            binding.forumDesc.text = it.desc
            binding.forumAvatar.displayImage(it.avatarUrl)
            binding.forumAvatar.setOnClickListener { _ ->
                if (it.avatarUrl != null) {
                    photoViewModel.currentIndex.value = 0
                    photoViewModel.photos = listOf(Photo(it.avatarUrl, 0, "rotieba"))
                    findNavController().navigate(MobileNavigationDirections.viewPhotos())
                }
            }
            if (!viewModel.historyAdded) {
                updateHistory()
                viewModel.historyAdded = true
            }
        }
        val threadAdapter = ThreadAdapter(ForumThreadUiStateComparator)
        threadAdapter.addLoadStateListener { state ->
            when (state.refresh) {
                is LoadState.Error -> {
                    val err = (state.refresh as LoadState.Error).error
                    if (!viewModel.forumInitialized) {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.error_dialog_title)
                            .setMessage(err.message)
                            .setOnDismissListener {
                                navigateUp()
                            }
                            .show()
                    } else {
                        binding.loadError.isVisible = true
                        binding.loadErrorText.text = err.message
                    }
                }

                else -> {
                    binding.loadError.isVisible = false
                }
            }
        }
        binding.loadErrorRefresh.setOnClickListener {
            threadAdapter.refresh()
        }
        binding.threadList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = threadAdapter
            addOnScrollListener(PauseLoadOnQuickScrollListener())
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.flow.collect {
                threadAdapter.submitData(it)
                threadAdapter.submitData(PagingData.empty())
            }
        }
        binding.forumTabLayout.apply {
            addOnTabSelectedListener(object : OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    if (tab.position == viewModel.tabPosition) return
                    viewModel.tabPosition = tab.position
                    binding.orderButton.isGone =
                        viewModel.tabs.value!![viewModel.tabPosition] is ForumTab.HotTab
                    threadAdapter.refresh()
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {
                }

            })

            viewModel.tabs.observe(viewLifecycleOwner) { tabs ->
                removeAllTabs()
                tabs.forEach { forumTab ->
                    addTab(newTab().apply { text = forumTab.name }, false)
                }
                selectTab(getTabAt(viewModel.tabPosition))
            }
        }
        binding.orderButton.apply {
            viewModel.forumSortType.observe(viewLifecycleOwner) { sort ->
                text = when (sort) {
                    ForumSortType.REPLY_TIME -> getString(R.string.sort_by_reply_time)
                    ForumSortType.CREATE_TIME -> getString(R.string.sort_by_create_time)
                }
            }
            setOnClickListener { btn ->
                val popup = PopupMenu(btn.context, btn)
                popup.menuInflater.inflate(R.menu.forum_sort_menu, popup.menu)
                popup.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.sort_by_reply_time -> {
                            viewModel.forumSortType.value = ForumSortType.REPLY_TIME
                        }

                        R.id.sort_by_create_time -> {
                            viewModel.forumSortType.value = ForumSortType.CREATE_TIME
                        }
                    }
                    threadAdapter.refresh()
                    true
                }
                popup.show()
            }
        }
        return binding.root
    }

    private fun updateHistory() {
        val forumInfo = viewModel.forumInfo.value ?: return
        lifecycleScope.launch {
            App.instance.historyManager.updateHistory(
                HistoryEntry(
                    type = EntryType.FORUM,
                    id = forumInfo.id.toString(),
                    time = System.currentTimeMillis(),
                    forumName = forumInfo.name,
                    forumAvatar = forumInfo.avatarUrl!!
                )
            )
        }
    }


    inner class ThreadAdapter(diffCallback: DiffUtil.ItemCallback<ForumThreadUiState>) :
        PagingDataAdapter<ForumThreadUiState, ThreadAdapter.ThreadViewHolder>(
            diffCallback
        ) {
        inner class ThreadViewHolder(val binding: FragmentForumThreadItemBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onBindViewHolder(holder: ThreadViewHolder, position: Int) {
            val state = getItem(position) ?: return
            val thread = state.thread
            holder.binding.root.setData(thread)
            holder.binding.threadTitle.text = SpannableStringBuilder().apply {
                val context = requireContext()
                if (thread.isTop) {
                    append(
                        "[置顶]",
                        RoundSpan(
                            context,
                            context.getColor(R.color.top_span_background),
                            context.getColor(R.color.top_span_text),
                            showText = "置顶"
                        ),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    append(" ")
                }
                if (thread.threadType == ThreadType.HELP) {
                    append(
                        "[求助]",
                        RoundSpan(
                            context,
                            context.getColor(R.color.help_span_background),
                            context.getColor(R.color.help_span_text),
                            showText = "求助"
                        ),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    append(" ")
                }
                if (thread.isGood) {
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
                if (thread.tabInfo != null) {
                    append(
                        "${thread.tabInfo.name} | ",
                        StyleSpan(Typeface.BOLD),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                append(thread.title)
            }
            holder.binding.threadTitle.isGone = holder.binding.threadTitle.text.isEmpty()
            holder.binding.threadContent.isVisible = state.expanded
            holder.binding.threadAvatar.isVisible = state.expanded
            holder.binding.threadInfo.isVisible = state.expanded
            holder.binding.threadUserName.isVisible = state.expanded

            holder.binding.threadContent.text = SpannableStringBuilder()
                .appendSimpleContent(thread.content, requireContext())
            holder.binding.threadUserName.text = thread.author.nick.ifEmpty { thread.author.name }
            holder.binding.threadInfo.setOnClickListener {
                val (ks, vs) = thread.toDetail()
                DetailDialogFragment.newInstance(ks, vs).show(childFragmentManager, "detail")
            }
            holder.binding.threadAvatar.setOnClickListener {
                findNavController().navigate(
                    MobileNavigationDirections.showProfile(thread.author.uidOrPortrait)
                )
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
                append(thread.replyNum.toSimpleString())
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
                append(thread.viewNum.toSimpleString())
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
                append(thread.agreeNum.toSimpleString())
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
                append(thread.disagreeNum.toSimpleString())
            }
            holder.binding.threadAvatar.displayImageInList(thread.author.avatarUrl)
            holder.binding.cardRoot.setOnClickListener {
                findNavController().navigate(MobileNavigationDirections.goToThread(thread.tid))
            }
            val images = thread.images
            val image1 = images.firstOrNull()
            val image2 = images.getOrNull(1)
            val image3 = images.getOrNull(2)
            if (image1 != null) {
                holder.binding.previewImage1.visibility = View.VISIBLE
                holder.binding.previewImage1.displayImageInList(image1.previewSrc) {
                    configureDefaults(requireContext())
                }
                holder.binding.previewImage1.setOnClickListener {
                    photoViewModel.currentIndex.value = 0
                    photoViewModel.photos = images.map {
                        it.toPhoto(thread)
                    }
                    findNavController().navigate(MobileNavigationDirections.viewPhotos())
                }
                holder.binding.previewImage1.setOnLongClickListener {
                    it.setSelectedData(image1.toPhoto(thread))
                    false
                }
            } else {
                holder.binding.previewImage1.visibility = View.INVISIBLE
                holder.binding.previewImage1.setOnClickListener(null)
                holder.binding.previewImage1.setOnLongClickListener(null)
            }
            if (image2 != null) {
                holder.binding.previewImage2.visibility = View.VISIBLE
                holder.binding.previewImage2.displayImageInList(image2.previewSrc) {
                    configureDefaults(requireContext())
                }
                holder.binding.previewImage2.setOnClickListener {
                    photoViewModel.currentIndex.value = 1
                    photoViewModel.photos = images.map {
                        it.toPhoto(thread)
                    }
                    findNavController().navigate(MobileNavigationDirections.viewPhotos())
                }
                holder.binding.previewImage2.setOnLongClickListener {
                    it.setSelectedData(image2.toPhoto(thread))
                    false
                }
            } else {
                holder.binding.previewImage2.visibility = View.INVISIBLE
                holder.binding.previewImage2.setOnClickListener(null)
                holder.binding.previewImage2.setOnLongClickListener(null)
            }
            if (image3 != null) {
                holder.binding.previewImage3.visibility = View.VISIBLE
                holder.binding.previewImage3.displayImageInList(image3.previewSrc) {
                    configureDefaults(requireContext())
                }
                holder.binding.previewImage3.setOnClickListener {
                    photoViewModel.currentIndex.value = 2
                    photoViewModel.photos = images.map {
                        it.toPhoto(thread)
                    }
                    findNavController().navigate(MobileNavigationDirections.viewPhotos())
                }
                holder.binding.previewImage3.setOnLongClickListener {
                    it.setSelectedData(image3.toPhoto(thread))
                    false
                }
            } else {
                holder.binding.previewImage3.visibility = View.INVISIBLE
                holder.binding.previewImage3.setOnClickListener(null)
                holder.binding.previewImage3.setOnLongClickListener(null)
            }
            val noImage = images.isEmpty()
            if (noImage || !state.expanded) {
                holder.binding.previewImage1.visibility = View.GONE
                holder.binding.previewImage2.visibility = View.GONE
                holder.binding.previewImage3.visibility = View.GONE
            }
            holder.binding.imageSpace.isGone = noImage || !state.expanded
            holder.binding.threadTitle.maxLines = if (state.expanded) Int.MAX_VALUE else 1
            holder.binding.expandBtn.apply {
                isVisible = thread.isTop
                setImageResource(if (state.expanded) R.drawable.ic_up else R.drawable.ic_down)
                setOnClickListener {
                    state.expanded = !state.expanded
                    setImageResource(if (state.expanded) R.drawable.ic_up else R.drawable.ic_down)
                    holder.binding.threadContent.isVisible = state.expanded
                    holder.binding.threadAvatar.isVisible = state.expanded
                    holder.binding.threadInfo.isVisible = state.expanded
                    holder.binding.threadUserName.isVisible = state.expanded
                    holder.binding.previewImage1.visibility =
                        if (!noImage && state.expanded) if (image1 != null) View.VISIBLE else View.INVISIBLE else View.GONE
                    holder.binding.previewImage2.visibility =
                        if (!noImage && state.expanded) if (image2 != null) View.VISIBLE else View.INVISIBLE else View.GONE
                    holder.binding.previewImage3.visibility =
                        if (!noImage && state.expanded) if (image3 != null) View.VISIBLE else View.INVISIBLE else View.GONE
                    holder.binding.imageSpace.isGone = noImage || !state.expanded
                    holder.binding.threadTitle.maxLines = if (state.expanded) Int.MAX_VALUE else 1
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreadViewHolder {
            return ThreadViewHolder(
                FragmentForumThreadItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ).apply {
                    registerForContextMenu(root)
                    cardRoot.setOnLongClickListener { false }
                }
            )
        }
    }
}

object ForumThreadUiStateComparator : DiffUtil.ItemCallback<ForumThreadUiState>() {
    override fun areItemsTheSame(
        oldItem: ForumThreadUiState,
        newItem: ForumThreadUiState
    ): Boolean {
        return oldItem.thread.tid == newItem.thread.tid
    }

    override fun areContentsTheSame(
        oldItem: ForumThreadUiState,
        newItem: ForumThreadUiState
    ): Boolean {
        return oldItem == newItem
    }
}