package io.github.a13e300.ro_tieba.ui.thread

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.github.panpf.sketch.displayImage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.BaseFragment
import io.github.a13e300.ro_tieba.Emotions
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.PhotoUtils
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.appendSimpleContent
import io.github.a13e300.ro_tieba.databinding.FragmentThreadBinding
import io.github.a13e300.ro_tieba.databinding.FragmentThreadCommentPreviewBinding
import io.github.a13e300.ro_tieba.databinding.FragmentThreadHeaderBinding
import io.github.a13e300.ro_tieba.databinding.FragmentThreadPostItemBinding
import io.github.a13e300.ro_tieba.databinding.ImageContentBinding
import io.github.a13e300.ro_tieba.databinding.ThreadListFooterBinding
import io.github.a13e300.ro_tieba.forceShowIcon
import io.github.a13e300.ro_tieba.misc.EmojiSpan
import io.github.a13e300.ro_tieba.misc.IconSpan
import io.github.a13e300.ro_tieba.misc.MyURLSpan
import io.github.a13e300.ro_tieba.misc.PlaceHolderDrawable
import io.github.a13e300.ro_tieba.models.Comment
import io.github.a13e300.ro_tieba.models.Content
import io.github.a13e300.ro_tieba.models.IPost
import io.github.a13e300.ro_tieba.models.Post
import io.github.a13e300.ro_tieba.toSimpleString
import io.github.a13e300.ro_tieba.ui.DetailDialogFragment
import io.github.a13e300.ro_tieba.ui.photo.Photo
import io.github.a13e300.ro_tieba.ui.photo.PhotoViewModel
import io.github.a13e300.ro_tieba.ui.photo.TRANSITION_NAME_PREFIX
import io.github.a13e300.ro_tieba.ui.toDetail
import io.github.a13e300.ro_tieba.utils.appendUserInfo
import io.github.a13e300.ro_tieba.view.ItemView
import io.github.a13e300.ro_tieba.view.MyLinkMovementMethod
import io.github.a13e300.ro_tieba.view.SelectedLink
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ThreadFragment : BaseFragment() {

    private val viewModel: ThreadViewModel by viewModels()
    private val photoViewModel: PhotoViewModel by viewModels({ findNavController().currentBackStackEntry!! })
    private val args: ThreadFragmentArgs by navArgs()
    private lateinit var binding: FragmentThreadBinding
    private lateinit var postAdapter: PostAdapter
    private lateinit var headerAdapter: HeaderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentThreadBinding.inflate(inflater, container, false)
        if (viewModel.threadConfig.value == null) {
            viewModel.threadConfig.value = ThreadConfig(args.tid, args.pid)
        }
        postAdapter = PostAdapter(PostComparator)
        postAdapter.addLoadStateListener { state ->
            (state.refresh as? LoadState.Error)?.error?.let {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.error_dialog_title)
                    .setMessage(it.message)
                    .setOnDismissListener {
                        findNavController().navigateUp()
                    }
                    .show()
            }
        }
        headerAdapter = HeaderAdapter()
        viewModel.needLoadPrevious.observe(viewLifecycleOwner) {
            headerAdapter.notifyDataSetChanged()
        }
        binding.list.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = ConcatAdapter(
                headerAdapter,
                postAdapter
            )
            addItemDecoration(
                MyItemDecoration(
                    resources.getDimension(R.dimen.thread_list_margin).toInt()
                )
            )
        }
        viewModel.threadInfo.observe(viewLifecycleOwner) {
            binding.toolbar.title = it.forum?.name
            headerAdapter.notifyDataSetChanged()
        }
        setupToolbar(binding.toolbar)
        binding.toolbar.setOnClickListener {
            binding.list.scrollToPosition(0)
        }
        binding.toolbar.addMenuProvider(object : MenuProvider {
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.refresh) {
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
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val currentUid = App.instance.accountManager.currentAccount.first().uid
                if (viewModel.currentUid == null)
                    viewModel.currentUid = currentUid
                else if (currentUid != viewModel.currentUid) {
                    findNavController().navigateUp()
                    return@repeatOnLifecycle
                }
                viewModel.flow.collect { data ->
                    postAdapter.submitData(data)
                }
            }
        }
        return binding.root
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        MenuInflater(requireContext()).inflate(R.menu.post_item_menu, menu)
        menu.forceShowIcon()
        val selected = (menuInfo as? ItemView.ContextMenuInfo)?.selectedData
        if (selected is SelectedLink) {
            menu.setGroupVisible(R.id.group_link, true)
        }
        if (selected is Photo) {
            menu.setGroupVisible(R.id.group_photo, true)
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as? ItemView.ContextMenuInfo
        val selected = info?.selectedData
        val post = if (selected is IPost) selected else info?.data as? IPost
        if (post != null) {
            when (item.itemId) {
                R.id.copy_post_content -> {
                    val cm = requireContext().getSystemService(ClipboardManager::class.java)
                    cm.setPrimaryClip(ClipData.newPlainText("", post.content.joinToString("") {
                        when (it) {
                            is Content.TextContent -> it.text
                            is Content.ImageContent -> "[${it.src}]"
                            is Content.LinkContent -> "[${it.text}](${it.link})"
                            is Content.EmojiContent -> Emotions.emotionMap.get(it.id)?.name ?: it.id
                        }
                    }))
                    return true
                }

                R.id.copy_post_link -> {
                    val cm = requireContext().getSystemService(ClipboardManager::class.java)
                    val text = when (post) {
                        is Post -> "https://tieba.baidu.com/p/${post.tid}?pid=${post.postId}"
                        is Comment -> "https://tieba.baidu.com/p/${post.tid}?pid=${post.postId}&ppid=${post.ppid}"
                        else -> null
                    }
                    cm.setPrimaryClip(
                        ClipData.newPlainText(
                            "",
                            text
                        )
                    )
                    return true
                }

                R.id.open_link -> {
                    (selected as? SelectedLink)?.url?.also {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                    }
                    return true
                }

                R.id.copy_link -> {
                    (selected as? SelectedLink)?.url?.also {
                        val cm = requireContext().getSystemService(ClipboardManager::class.java)
                        cm.setPrimaryClip(
                            ClipData.newPlainText(
                                "",
                                it
                            )
                        )
                    }
                    return true
                }

                R.id.save_photo -> {
                    (selected as? Photo)?.let { photo ->
                        lifecycleScope.launch {
                            PhotoUtils.downloadPhoto(
                                activity = requireActivity(),
                                photo = photo,
                                onSuccess = {
                                    Snackbar.make(
                                        binding.root,
                                        getString(R.string.saved_to_gallery),
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                },
                                onFailure = {
                                    Snackbar.make(
                                        binding.root,
                                        "error:${it.message}",
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                    return true
                }

                R.id.share_photo -> {
                    lifecycleScope.launch {
                        (selected as? Photo)?.let { photo ->
                            PhotoUtils.sharePhoto(
                                context = requireContext(),
                                photo = photo,
                                onFailure = {
                                    Snackbar.make(
                                        binding.root,
                                        "failed to share:${it.message}",
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                    return true
                }
            }
        }
        return super.onContextItemSelected(item)
    }

    class MyItemDecoration(private val mMargin: Int) : ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val pos = parent.getChildAdapterPosition(view)
            if (pos >= 2) {
                outRect.set(0, mMargin, 0, 0)
            }
        }
    }

    class HeaderHolder(val binding: FragmentThreadHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class HeaderAdapter : RecyclerView.Adapter<HeaderHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderHolder =
            HeaderHolder(FragmentThreadHeaderBinding.inflate(layoutInflater, parent, false))

        override fun getItemCount(): Int = 1

        override fun onBindViewHolder(holder: HeaderHolder, position: Int) {
            val thread = viewModel.threadInfo.value ?: return
            holder.binding.threadTitle.text = thread.title
            viewModel.needLoadPrevious.value?.let { holder.binding.loadPrevious.isGone = !it }
            holder.binding.loadPrevious.setOnClickListener {
                viewModel.threadConfig.value = viewModel.threadConfig.value!!.copy(pid = 0L)
                postAdapter.refresh()
            }
            holder.binding.threadInfo.text = SpannableStringBuilder().apply {
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
            }
        }

    }

    class FooterHolder(val binding: ThreadListFooterBinding) : RecyclerView.ViewHolder(binding.root)

    inner class FooterAdapter : LoadStateAdapter<FooterHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): FooterHolder {
            return FooterHolder(ThreadListFooterBinding.inflate(layoutInflater, parent, false))
        }

        override fun onBindViewHolder(holder: FooterHolder, loadState: LoadState) {
            val isErr = loadState is LoadState.Error
            holder.binding.retryButton.isVisible = isErr
            holder.binding.errorMessage.isVisible = isErr
            if (loadState is LoadState.Error) {
                holder.binding.errorMessage.text = loadState.error.message
            }
        }
    }

    class PostViewHolder(val binding: FragmentThreadPostItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class PostAdapter(diffCallback: DiffUtil.ItemCallback<Post>) :
        PagingDataAdapter<Post, PostViewHolder>(
            diffCallback
        ) {

        override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
            val post = getItem(position) ?: return
            holder.binding.root.apply {
                setData(post)
                setOnClickListener {
                    findNavController().navigate(
                        MobileNavigationDirections.showComments(
                            post.tid,
                            post.postId
                        )
                    )
                }
            }
            val context = requireContext()
            holder.binding.accountName.text = post.user.showName
            holder.binding.accountInfo.text = SpannableStringBuilder().appendUserInfo(
                post.user, post.user.uid == viewModel.threadInfo.value?.author?.uid,
                context,
                showLevel = true
            )
            val contentView = holder.binding.content
            val fontSize = context.resources.getDimensionPixelSize(R.dimen.content_text_size)
            contentView.removeAllViews()
            var lastString: SpannableStringBuilder? = null
            holder.binding.avatar.displayImage(post.user.avatarUrl)
            fun addTextView() {
                if (lastString == null) return
                contentView.addView(AppCompatTextView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    text = lastString
                    movementMethod = MyLinkMovementMethod
                    isClickable = false
                    isLongClickable = false
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize.toFloat())
                })
                lastString = null
            }
            for (content in post.content) {
                when (content) {
                    is Content.TextContent -> {
                        if (lastString == null) lastString = SpannableStringBuilder()
                        lastString!!.append(content.text)
                    }

                    is Content.LinkContent -> {
                        if (lastString == null) lastString = SpannableStringBuilder()
                        lastString!!.append(
                            content.text.ifEmpty { "[link]" },
                            MyURLSpan(content.link),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                    is Content.ImageContent -> {
                        addTextView()
                        val imageView =
                            ImageContentBinding.inflate(layoutInflater, contentView, false)
                                .root.apply {
                                    Logger.d("scaleType=${scaleType}")
                                    displayImage(content.previewSrc) {
                                        placeholder(
                                            PlaceHolderDrawable(
                                                content.width,
                                                content.height
                                            )
                                        )
                                        resize(content.width, content.height)
                                    }
                                    val idx =
                                        viewModel.photos.keys.indexOf(post.floor to content.order)
                                    val name = "${TRANSITION_NAME_PREFIX}_$idx"
                                    ViewCompat.setTransitionName(this, name)
                                    setOnClickListener {
                                        photoViewModel.photos = viewModel.photos.values.toList()
                                        photoViewModel.currentIndex.value = idx
                                        findNavController().navigate(
                                            MobileNavigationDirections.viewPhotos(),
                                            // navigatorExtras = FragmentNavigatorExtras(it to name)
                                        )

                                    }
                                    setOnLongClickListener {
                                        var parent = it.parent
                                        while (parent !is ItemView) parent = parent.parent
                                        (parent as? ItemView)?.setSelectedData(
                                            Photo(
                                                content.src, content.order, post
                                            )
                                        )
                                        false
                                    }
                                }
                        contentView.addView(imageView)
                    }

                    is Content.EmojiContent -> {
                        if (lastString == null) lastString = SpannableStringBuilder()
                        val emoji = Emotions.emotionMap.get(content.id)
                        if (emoji == null) {
                            lastString!!.append("[${content.id}]")
                        } else {
                            val drawable =
                                AppCompatResources.getDrawable(requireContext(), emoji.resource)!!
                            lastString!!.append(
                                emoji.name,
                                EmojiSpan(drawable),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                    }

                    else -> {}
                }
            }
            addTextView()
            holder.binding.floorNum.setOnClickListener {
                val (ks, vs) = post.toDetail()
                DetailDialogFragment.newInstance(ks, vs).show(childFragmentManager, "detail")
            }
            holder.binding.floorNum.text = SpannableStringBuilder().apply {
                append("${post.floor}楼·")
                append(
                    "时间 ",
                    IconSpan(
                        AppCompatResources.getDrawable(
                            requireContext(),
                            R.drawable.ic_time
                        )!!
                    ),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                append(post.time.toSimpleString())
                append("·")
                append(
                    "地理位置 ",
                    IconSpan(
                        AppCompatResources.getDrawable(
                            requireContext(),
                            R.drawable.ic_location
                        )!!
                    ),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                append(post.user.location)
            }
            val hasComment = post.commentCount != 0
            holder.binding.commentsBox.isGone = !hasComment
            if (hasComment) {
                holder.binding.commentsContent.removeAllViews()
                post.comments.forEach { comment ->
                    val preview = FragmentThreadCommentPreviewBinding.inflate(layoutInflater)
                    val sb = SpannableStringBuilder()
                    sb.append(
                        comment.user.showName,
                        StyleSpan(Typeface.BOLD),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    sb.appendUserInfo(
                        comment.user,
                        comment.user.uid == viewModel.threadInfo.value?.author?.uid,
                        requireContext()
                    )
                    sb.append(": ")
                    sb.appendSimpleContent(comment.content, requireContext())
                    preview.text.text = sb
                    preview.root.setOnLongClickListener {
                        var parent = it.parent
                        while (parent !is ItemView) parent = parent.parent
                        (parent as? ItemView)?.setSelectedData(comment)
                        false
                    }
                    holder.binding.commentsContent.addView(preview.root)
                }
                if (post.commentCount > 4) {
                    val preview = FragmentThreadCommentPreviewBinding.inflate(layoutInflater)
                    preview.text.text = "查看全部${post.commentCount}条回复"
                    holder.binding.commentsContent.addView(preview.root)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
            return PostViewHolder(
                FragmentThreadPostItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ).apply { registerForContextMenu(root) }
            )
        }
    }
}

object PostComparator : DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem.postId == newItem.postId
    }

    override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem == newItem
    }
}