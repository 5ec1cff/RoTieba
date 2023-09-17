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
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.MediaController
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.github.panpf.sketch.displayImage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.BaseFragment
import io.github.a13e300.ro_tieba.EXTRA_DONT_USE_NAV
import io.github.a13e300.ro_tieba.Emotions
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.PhotoUtils
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.appendSimpleContent
import io.github.a13e300.ro_tieba.databinding.DialogJumpPageBinding
import io.github.a13e300.ro_tieba.databinding.FragmentThreadBinding
import io.github.a13e300.ro_tieba.databinding.FragmentThreadCommentPreviewBinding
import io.github.a13e300.ro_tieba.databinding.FragmentThreadHeaderBinding
import io.github.a13e300.ro_tieba.databinding.FragmentThreadPostItemBinding
import io.github.a13e300.ro_tieba.databinding.ImageContentBinding
import io.github.a13e300.ro_tieba.databinding.ThreadListFooterBinding
import io.github.a13e300.ro_tieba.databinding.VideoViewBinding
import io.github.a13e300.ro_tieba.forceShowIcon
import io.github.a13e300.ro_tieba.misc.EmojiSpan
import io.github.a13e300.ro_tieba.misc.IconSpan
import io.github.a13e300.ro_tieba.misc.MyURLSpan
import io.github.a13e300.ro_tieba.misc.OnPreImeBackPressedListener
import io.github.a13e300.ro_tieba.misc.PlaceHolderDrawable
import io.github.a13e300.ro_tieba.models.Comment
import io.github.a13e300.ro_tieba.models.Content
import io.github.a13e300.ro_tieba.models.IPost
import io.github.a13e300.ro_tieba.models.Post
import io.github.a13e300.ro_tieba.openPostAtOtherClient
import io.github.a13e300.ro_tieba.toSimpleString
import io.github.a13e300.ro_tieba.ui.DetailDialogFragment
import io.github.a13e300.ro_tieba.ui.photo.Photo
import io.github.a13e300.ro_tieba.ui.photo.PhotoViewModel
import io.github.a13e300.ro_tieba.ui.toDetail
import io.github.a13e300.ro_tieba.utils.appendUserInfo
import io.github.a13e300.ro_tieba.view.ContentTextView
import io.github.a13e300.ro_tieba.view.ItemView
import io.github.a13e300.ro_tieba.view.SelectedLink
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ThreadFragment : BaseFragment() {

    private val viewModel: ThreadViewModel by viewModels()
    private val photoViewModel: PhotoViewModel by viewModels({ findNavController().currentBackStackEntry!! })
    private val args: ThreadFragmentArgs by navArgs()
    private lateinit var binding: FragmentThreadBinding
    private lateinit var postAdapter: PostAdapter
    private lateinit var postLayoutManager: LinearLayoutManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentThreadBinding.inflate(inflater, container, false)
        if (savedInstanceState == null) {
            viewModel.threadConfig = ThreadConfig(args.tid, args.pid)
        }
        postAdapter = PostAdapter(PostComparator)
        postAdapter.addLoadStateListener { state ->
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
        postLayoutManager = LinearLayoutManager(context)
        binding.list.apply {
            layoutManager = postLayoutManager
            adapter = postAdapter
            addItemDecoration(
                MyItemDecoration(
                    resources.getDimension(R.dimen.thread_list_margin).toInt()
                )
            )
        }
        viewModel.threadInfo.observe(viewLifecycleOwner) {
            binding.toolbar.title = it.forum?.name
        }
        setupToolbar(binding.toolbar)
        binding.toolbar.setOnClickListener {
            binding.list.scrollToPosition(0)
        }
        binding.toolbar.addMenuProvider(object : MenuProvider {
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.refresh -> {
                        viewModel.threadConfig = viewModel.threadConfig.copy(pid = 0L)
                        postAdapter.refresh()
                        true
                    }

                    R.id.sort -> {
                        val v = !menuItem.isChecked
                        menuItem.setChecked(v)
                        viewModel.threadConfig = viewModel.threadConfig.copy(reverse = v)
                        postAdapter.refresh()
                        true
                    }

                    R.id.see_lz -> {
                        val v = !menuItem.isChecked
                        menuItem.setChecked(v)
                        // clear pid to prevent from bugs
                        viewModel.threadConfig = viewModel.threadConfig.copy(seeLz = v, pid = 0L)
                        postAdapter.refresh()
                        true
                    }

                    R.id.jump_page -> {
                        handleJumpPage()
                        true
                    }

                    else -> false
                }
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.thread_menu, menu)
                menu.findItem(R.id.sort).setChecked(viewModel.threadConfig.reverse)
                menu.findItem(R.id.see_lz).setChecked(viewModel.threadConfig.seeLz)
            }
        })
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val currentUid = App.instance.accountManager.currentAccount.first().uid
                if (viewModel.currentUid == null)
                    viewModel.currentUid = currentUid
                else if (currentUid != viewModel.currentUid) {
                    navigateUp()
                    return@repeatOnLifecycle
                }
                viewModel.flow.collect { data ->
                    postAdapter.submitData(data)
                }
            }
        }
        return binding.root
    }

    private fun handleJumpPage() {
        val totalPage = viewModel.totalPage
        val page = postLayoutManager.findFirstVisibleItemPosition().let {
            if (it == RecyclerView.NO_POSITION) 0
            else {
                val items = postAdapter.snapshot().items
                (items[it].let { a ->
                    if (a is ThreadViewModel.PostModel.Header) {
                        items.getOrNull(it + 1)?.let { b -> (b as? ThreadViewModel.PostModel.Post) }
                    } else (a as? ThreadViewModel.PostModel.Post)
                })?.post?.page ?: 0
            }
        }
        val title = if (page != 0) "第 $page / $totalPage 页" else "共 $totalPage 页"
        val b = DialogJumpPageBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(b.root)
            .create()

        fun toPage(t: String?) =
            if (t.isNullOrEmpty()) 0
            else t.toIntOrNull()?.let { if (it in 1..totalPage) it else 0 } ?: 0

        fun jump(pn: Int) {
            viewModel.threadConfig = viewModel.threadConfig.copy(pid = 0L, page = pn)
            postAdapter.refresh()
            dialog.dismiss()
        }
        b.inputText.doAfterTextChanged {
            val canJump = toPage(it?.toString()) != 0
            b.buttonOk.isEnabled = canJump
            b.inputLayout.error = if (canJump) null else "请输入 1 到 $totalPage 的整数"
        }
        b.inputText.setOnEditorActionListener { textView, i, keyEvent ->
            Logger.d("OnEditorActionListener $i $keyEvent")
            if (i != EditorInfo.IME_ACTION_DONE &&
                !(keyEvent?.action == KeyEvent.ACTION_DOWN
                        && keyEvent.keyCode in intArrayOf(
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_NUMPAD_ENTER
                )
                        )
            )
                return@setOnEditorActionListener false
            val pn = toPage(textView.text?.toString())
            if (pn != 0) jump(pn)
            true
        }
        b.buttonOk.setOnClickListener {
            jump(b.inputText.text.toString().toInt())
        }
        dialog.window?.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }
        dialog.show()
        b.inputText.requestFocus()
        b.root.onBackPressedListener = OnPreImeBackPressedListener {
            it.context.getSystemService(InputMethodManager::class.java)
                .hideSoftInputFromWindow(it.windowToken, 0)
            dialog.hide()
            true
        }
        // context.getSystemService(InputMethodManager::class.java).showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
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
        ((menuInfo as? ItemView.ContextMenuInfo)?.data as? IPost)?.content?.find { it is Content.VideoContent }
            ?.let { menu.setGroupVisible(R.id.group_video, true) }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as? ItemView.ContextMenuInfo
        val selected = info?.selectedData
        val post = if (selected is IPost) selected else info?.data as? IPost
        val video = post?.content?.find { it is Content.VideoContent } as? Content.VideoContent
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
                            is Content.VideoContent -> "[video](${it.src})"
                            is Content.UnknownContent -> it.source
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

                R.id.open_at_other_client -> {
                    val tid = when (post) {
                        is Post -> post.tid
                        is Comment -> post.tid
                        else -> return false
                    }
                    val pid = when (post) {
                        is Post -> post.postId
                        is Comment -> post.postId
                        else -> return false
                    }
                    if (!openPostAtOtherClient(tid, pid, requireContext())) {
                        Snackbar.make(
                            binding.root,
                            getString(R.string.no_other_apps_tips),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    return true
                }

                R.id.open_link -> {
                    (selected as? SelectedLink)?.url?.also {
                        startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(it)),
                            bundleOf(EXTRA_DONT_USE_NAV to true)
                        )
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

                R.id.save_video -> {
                    video?.src?.let {
                        lifecycleScope.launch {
                            PhotoUtils.downloadVideo(
                                activity = requireActivity(),
                                url = it,
                                post = post as Post,
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

                R.id.open_video -> {
                    video?.text?.let {
                        startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(it)),
                            bundleOf(EXTRA_DONT_USE_NAV to true)
                        )
                    }
                    return true
                }

                R.id.copy_video_link -> {
                    video?.src?.also {
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

    class FooterHolder(val binding: ThreadListFooterBinding) : ViewHolder(binding.root)

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
        ViewHolder(binding.root)

    class HeaderViewHolder(val binding: FragmentThreadHeaderBinding) :
        ViewHolder(binding.root)


    inner class PostAdapter(diffCallback: DiffUtil.ItemCallback<ThreadViewModel.PostModel>) :
        PagingDataAdapter<ThreadViewModel.PostModel, ViewHolder>(
            diffCallback
        ) {

        override fun getItemViewType(position: Int): Int {
            return when (peek(position)) {
                is ThreadViewModel.PostModel.Post -> R.layout.fragment_thread_post_item
                is ThreadViewModel.PostModel.Header -> R.layout.fragment_thread_header
                else -> throw IllegalStateException("unknown item")
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val post = getItem(position) ?: return
            if (holder is PostViewHolder) {
                bindForPost(holder, (post as ThreadViewModel.PostModel.Post).post)
            } else if (holder is HeaderViewHolder) {
                bindForHeader(holder)
            }
        }

        private fun bindForHeader(holder: HeaderViewHolder) {
            val thread = viewModel.threadInfo.value ?: return
            holder.binding.threadTitle.text = thread.title
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
        }

        private fun bindForPost(holder: PostViewHolder, post: Post) {

            fun showComments() {
                findNavController().navigate(
                    MobileNavigationDirections.showComments(
                        post.tid,
                        post.postId
                    )
                )
            }
            holder.binding.root.apply {
                setData(post)
                if (post.commentCount > 0)
                    setOnClickListener {
                        showComments()
                    }
                else
                    setOnClickListener(null)
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
            holder.binding.avatar.setOnClickListener {
                // openUserAtOtherClient(post.user, requireContext())
                findNavController().navigate(
                    MobileNavigationDirections.showProfile().setPortrait(post.user.portrait)
                        .setUid(post.user.uid)
                )
            }
            fun addTextView() {
                if (lastString == null) return
                contentView.addView(ContentTextView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    text = lastString
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
                                    displayImage(content.previewSrc) {
                                        placeholder(
                                            PlaceHolderDrawable(
                                                content.width,
                                                content.height
                                            )
                                        )
                                        resize(content.width, content.height)
                                    }
                                    setOnClickListener {
                                        val idx =
                                            viewModel.photos.keys.indexOf(post.floor to content.order)
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

                    is Content.VideoContent -> {
                        addTextView()
                        val videoView = VideoViewBinding.inflate(layoutInflater, contentView, false)
                        videoView.video.setVideoURI(Uri.parse(content.src))
                        videoView.video.setMediaController(MediaController(requireContext()))
                        (videoView.video.layoutParams as ConstraintLayout.LayoutParams).dimensionRatio =
                            "W,${content.height}:${content.width}"
                        // videoView.video.start()
                        videoView.previewImage.displayImage(content.previewSrc)
                        videoView.previewImage.setOnClickListener {
                            it.isGone = true
                            videoView.video.start()
                        }
                        contentView.addView(videoView.root)
                    }

                    is Content.UnknownContent -> {
                        if (lastString == null) lastString = SpannableStringBuilder()
                        if (content.text.isNotEmpty())
                            lastString!!.append(
                                content.text,
                                StyleSpan(Typeface.ITALIC),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        else
                            lastString!!.append(
                                " [unknown type ${content.type}] ",
                                StyleSpan(Typeface.ITALIC),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
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
                append("·")
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
                append(post.agreeNum.toString())
                append("·")
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
                append(post.disagreeNum.toString())
            }
            val hasComment = post.commentCount != 0
            holder.binding.commentsBox.isVisible = hasComment
            holder.binding.commentsContent.removeAllViews()
            if (hasComment) {
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
                    sb.appendSimpleContent(comment.content, requireContext(), useUrlSpan = true)
                    preview.text.text = sb
                    preview.root.setOnClickListener { showComments() }
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
                    preview.root.setOnClickListener { showComments() }
                    holder.binding.commentsContent.addView(preview.root)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return when (viewType) {
                R.layout.fragment_thread_post_item -> PostViewHolder(
                    FragmentThreadPostItemBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    ).apply { registerForContextMenu(root) }
                )

                R.layout.fragment_thread_header -> HeaderViewHolder(
                    FragmentThreadHeaderBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                )

                else -> throw IllegalStateException("unknown type $viewType")
            }
        }
    }
}

object PostComparator : DiffUtil.ItemCallback<ThreadViewModel.PostModel>() {
    override fun areItemsTheSame(
        oldItem: ThreadViewModel.PostModel,
        newItem: ThreadViewModel.PostModel
    ): Boolean {
        if (oldItem is ThreadViewModel.PostModel.Post && newItem is ThreadViewModel.PostModel.Post)
            return oldItem.post.postId == newItem.post.postId
        else if (oldItem == ThreadViewModel.PostModel.Header && newItem == ThreadViewModel.PostModel.Header)
            return true
        return false
    }

    override fun areContentsTheSame(
        oldItem: ThreadViewModel.PostModel,
        newItem: ThreadViewModel.PostModel
    ): Boolean {
        return oldItem == newItem
    }
}