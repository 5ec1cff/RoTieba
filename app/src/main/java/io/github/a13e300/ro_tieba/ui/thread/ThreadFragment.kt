package io.github.a13e300.ro_tieba.ui.thread

import android.graphics.Rect
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.TypedValue
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
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.BaseFragment
import io.github.a13e300.ro_tieba.Emotions
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.databinding.DialogJumpPageBinding
import io.github.a13e300.ro_tieba.databinding.FragmentThreadBarBinding
import io.github.a13e300.ro_tieba.databinding.FragmentThreadBinding
import io.github.a13e300.ro_tieba.databinding.FragmentThreadCommentPreviewBinding
import io.github.a13e300.ro_tieba.databinding.FragmentThreadHeaderBinding
import io.github.a13e300.ro_tieba.databinding.FragmentThreadPostItemBinding
import io.github.a13e300.ro_tieba.databinding.ImageContentBinding
import io.github.a13e300.ro_tieba.databinding.VideoViewBinding
import io.github.a13e300.ro_tieba.db.EntryType
import io.github.a13e300.ro_tieba.db.HistoryEntry
import io.github.a13e300.ro_tieba.misc.EmojiSpan
import io.github.a13e300.ro_tieba.misc.IconSpan
import io.github.a13e300.ro_tieba.misc.MyURLSpan
import io.github.a13e300.ro_tieba.misc.OnPreImeBackPressedListener
import io.github.a13e300.ro_tieba.misc.PauseLoadOnQuickScrollListener
import io.github.a13e300.ro_tieba.misc.UserSpan
import io.github.a13e300.ro_tieba.models.Content
import io.github.a13e300.ro_tieba.models.Post
import io.github.a13e300.ro_tieba.ui.DetailDialogFragment
import io.github.a13e300.ro_tieba.ui.photo.PhotoViewModel
import io.github.a13e300.ro_tieba.ui.photo.toPhoto
import io.github.a13e300.ro_tieba.ui.toDetail
import io.github.a13e300.ro_tieba.utils.appendSimpleContent
import io.github.a13e300.ro_tieba.utils.appendTextAutoLink
import io.github.a13e300.ro_tieba.utils.appendUserInfo
import io.github.a13e300.ro_tieba.utils.configureDefaults
import io.github.a13e300.ro_tieba.utils.configureImageForContent
import io.github.a13e300.ro_tieba.utils.copyText
import io.github.a13e300.ro_tieba.utils.displayImageInList
import io.github.a13e300.ro_tieba.utils.firstOrNullFrom
import io.github.a13e300.ro_tieba.utils.indexOfFrom
import io.github.a13e300.ro_tieba.utils.setSelectedData
import io.github.a13e300.ro_tieba.utils.toSimpleString
import io.github.a13e300.ro_tieba.view.ContentTextView
import io.github.a13e300.ro_tieba.view.SelectedUser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ThreadFragment : BaseFragment() {
    private data class LastSeenThreadInfo(
        val lastTid: Long,
        val lastPid: Long,
        val page: Int,
        val floor: Int,
        val offset: Int,
        val seeLz: Boolean,
        val reverse: Boolean
    )

    companion object {
        const val FIRST_PAGE = 0
        private var sLastSeenThreadInfo: LastSeenThreadInfo? = null
    }

    private val viewModel: ThreadViewModel by viewModels()
    private val photoViewModel: PhotoViewModel by viewModels({ findNavController().currentBackStackEntry!! })
    private val args: ThreadFragmentArgs by navArgs()
    private var mHighlightIdx: Int = -1
    private lateinit var binding: FragmentThreadBinding
    private lateinit var postAdapter: PostAdapter
    private lateinit var postLayoutManager: LinearLayoutManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentThreadBinding.inflate(inflater, container, false)
        if (!viewModel.initialized) {
            var seeLz = args.seeLz != 0
            var reverse = false
            var pid = args.pid
            val lastSeen = sLastSeenThreadInfo
            val pn = if (lastSeen != null && args.tid == lastSeen.lastTid
                && (!seeLz || lastSeen.seeLz) // TODO: set an unspecified value
                && (args.pid == 0L || args.pid == lastSeen.lastPid)
                && args.pn == FIRST_PAGE
            ) {
                    viewModel.scrollRequest = ThreadViewModel.ScrollRequest.ByFloor(
                        lastSeen.floor,
                        highlight = false,
                        offset = lastSeen.offset
                    )
                seeLz = lastSeen.seeLz
                reverse = lastSeen.reverse
                pid = 0L
                lastSeen.page
            } else if (args.pn > 0 && args.pid == 0L) args.pn else FIRST_PAGE
            viewModel.threadConfig =
                ThreadConfig(args.tid, pid, page = pn, seeLz = seeLz, reverse = reverse)
            viewModel.init()
            if (pid != 0L && viewModel.scrollRequest == null)
                viewModel.scrollRequest = ThreadViewModel.ScrollRequest.ByPid(pid)
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
            if (state.refresh is LoadState.NotLoading) notifyBarUpdate()
        }
        postLayoutManager = object : LinearLayoutManager(context) {
            override fun onLayoutChildren(
                recycler: RecyclerView.Recycler?,
                state: RecyclerView.State?
            ) {
                val request = viewModel.scrollRequest
                if (request != null) {
                    val items = postAdapter.snapshot().items
                    if (items.isEmpty()) return
                    val idx =
                        items.indexOfFirst {
                            when (request) {
                                is ThreadViewModel.ScrollRequest.ByPid ->
                                    it is ThreadViewModel.PostModel.Post && it.post.postId == request.pid

                                is ThreadViewModel.ScrollRequest.ByFloor ->
                                    (request.floor != -1 && it is ThreadViewModel.PostModel.Post && it.post.floor == request.floor)
                                            || (request.floor == -1 && it is ThreadViewModel.PostModel.Header)

                                is ThreadViewModel.ScrollRequest.ByPage ->
                                    it is ThreadViewModel.PostModel.Post && it.post.page == request.page
                            }
                        }
                    if (idx != -1) {
                        scrollToPositionWithOffset(idx, request.offset)
                        if (request.offsetToBar)
                            prepareScrollOffsetToBar()
                        if (request.highlight)
                            mHighlightIdx = idx
                    } else {
                        Logger.e("could not find the position of $request at first load!")
                    }
                    viewModel.scrollRequest = null
                }
                super.onLayoutChildren(recycler, state)
            }
        }
        binding.list.apply {
            layoutManager = postLayoutManager
            adapter = postAdapter
            addItemDecoration(
                MyItemDecoration(
                    resources.getDimension(R.dimen.thread_list_margin).toInt()
                )
            )
            addOnScrollListener(PauseLoadOnQuickScrollListener())
            addOnScrollListener(mScrollListener)
        }
        viewModel.threadInfo.observe(viewLifecycleOwner) {
            binding.toolbar.title = it.forum?.name
            notifyBarUpdate()
            if (!viewModel.historyAdded) {
                updateHistory()
                viewModel.historyAdded = true
            }
        }
        setupToolbar(binding.toolbar)
        bindBar(binding.includeThreadBar)
        binding.toolbar.setOnClickListener {
            binding.list.scrollToPosition(0)
        }
        binding.toolbar.addMenuProvider(object : MenuProvider {
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.refresh -> {
                        viewModel.threadConfig = viewModel.threadConfig.copy(pid = 0L, page = 0)
                        viewModel.invalidateCache()
                        postAdapter.refresh()
                        true
                    }

                    R.id.sort -> {
                        val v = !menuItem.isChecked
                        setReverse(v)
                        true
                    }

                    R.id.see_lz -> {
                        val v = !menuItem.isChecked
                        setSeeLz(v)
                        true
                    }

                    R.id.jump_page -> {
                        handleJumpPage()
                        true
                    }

                    R.id.open_forum -> {
                        viewModel.threadInfo.value?.forum?.name?.let {
                            findNavController().navigate(MobileNavigationDirections.goToForum(it))
                        }
                        true
                    }

                    R.id.copy_link -> {
                        viewModel.threadInfo.value?.let {
                            copyText("https://tieba.baidu.com/p/${it.tid}?pid=${it.postId}${if (viewModel.threadConfig.seeLz) "&see_lz=1" else ""}")
                        }
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

    private fun setSeeLz(v: Boolean) {
        viewModel.threadConfig =
            viewModel.threadConfig.copy(seeLz = v, pid = 0L, page = 0)
        notifyBarUpdate()
        postAdapter.refresh()
        binding.toolbar.menu.findItem(R.id.see_lz).setChecked(viewModel.threadConfig.seeLz)
    }

    private fun setReverse(v: Boolean) {
        viewModel.threadConfig = viewModel.threadConfig.copy(reverse = v)
        notifyBarUpdate()
        postAdapter.refresh()
        binding.toolbar.menu.findItem(R.id.sort).setChecked(viewModel.threadConfig.reverse)
    }

    private fun notifyBarUpdate() {
        val barIdx =
            postAdapter.snapshot().items.indexOfFirst { it is ThreadViewModel.PostModel.Bar }
        if (barIdx != -1)
            postAdapter.notifyItemChanged(barIdx)
        updateBar(binding.includeThreadBar)
    }

    private fun bindBar(bar: FragmentThreadBarBinding) {
        bar.seeLzBtn.setOnClickListener {
            setSeeLz(!bar.seeLzBtn.isChecked)
        }
        bar.sortBtn.setOnClickListener {
            setReverse(!bar.sortBtn.isChecked)
        }
        bar.jumpBtn.setOnClickListener {
            handleJumpPage()
        }
    }

    private fun updateBar(bar: FragmentThreadBarBinding) {
        val data = viewModel.threadInfo.value ?: return
        val current = findCurrentPost()
        bar.count.text = "${data.replyNum} 条回复"
        bar.seeLzBtn.isChecked = viewModel.threadConfig.seeLz
        bar.sortBtn.isChecked = viewModel.threadConfig.reverse
        bar.jumpBtn.text = "第 ${current?.page} / ${viewModel.totalPage} 页"
    }

    private var mScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            val pos = postLayoutManager.findFirstVisibleItemPosition()
            if (pos == RecyclerView.NO_POSITION) return
            val shouldHide = when (val item = postAdapter.snapshot()[pos]) {
                is ThreadViewModel.PostModel.Header -> true
                is ThreadViewModel.PostModel.Bar -> false
                is ThreadViewModel.PostModel.Post -> {
                    item.post.floor == 1
                }

                null -> return
            }
            updateBar(binding.includeThreadBar)
            binding.mainStickyContainerLayout.postOnAnimation {
                binding.mainStickyContainerLayout.isGone = shouldHide
            }
        }
    }

    private val mScrollOffsetToBarListener =
        object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                view: View,
                p1: Int,
                p2: Int,
                p3: Int,
                p4: Int,
                p5: Int,
                p6: Int,
                p7: Int,
                p8: Int
            ) {
                val h = view.height
                binding.list.scrollBy(0, -h)
                view.removeOnLayoutChangeListener(this)
            }
        }

    private fun prepareScrollOffsetToBar() {
        binding.mainStickyContainerLayout.addOnLayoutChangeListener(mScrollOffsetToBarListener)
    }

    private fun handleJumpPage() {
        val totalPage = viewModel.totalPage
        val page = findCurrentPost()?.page ?: 0
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
            viewModel.scrollRequest = ThreadViewModel.ScrollRequest.ByPage(pn)
            postAdapter.refresh()
            dialog.dismiss()
        }
        b.inputText.doAfterTextChanged {
            val canJump = toPage(it?.toString()) != 0
            b.buttonOk.isEnabled = canJump
            b.inputLayout.error = if (canJump) null else "请输入 1 到 $totalPage 的整数"
        }
        b.inputText.setOnEditorActionListener { textView, i, keyEvent ->
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

    private fun saveLastSeenInfo() {
        val pos = postLayoutManager.findFirstVisibleItemPosition().let {
            if (it == RecyclerView.NO_POSITION) return
            postAdapter.snapshot().items.indexOfFrom(it) { item -> item is ThreadViewModel.PostModel.Post }
        }
        if (pos == -1) return
        val off = postLayoutManager.findViewByPosition(pos)?.top ?: return
        val item = postAdapter.snapshot().items[pos] as ThreadViewModel.PostModel.Post
        val page = item.post.page
        val floor = item.post.floor
        // TODO: remove floor = -1
        sLastSeenThreadInfo = LastSeenThreadInfo(
            args.tid, args.pid,
            page, floor, off, viewModel.threadConfig.seeLz, viewModel.threadConfig.reverse
        )
    }

    private fun findCurrentPost() = postLayoutManager.findFirstVisibleItemPosition().let {
        if (it == RecyclerView.NO_POSITION) {
            null
        } else {
            (postAdapter.snapshot().items.firstOrNullFrom(it) { item -> item is ThreadViewModel.PostModel.Post }
                    as? ThreadViewModel.PostModel.Post)?.post
        }
    }

    private fun updateHistory() {
        val info = viewModel.threadInfo.value ?: return
        val p = findCurrentPost()
        lifecycleScope.launch {
            App.instance.historyManager.updateHistory(
                HistoryEntry(
                    type = EntryType.THREAD,
                    id = info.tid.toString(),
                    time = System.currentTimeMillis(),
                    title = info.title,
                    forumName = info.forum!!.name,
                    forumAvatar = info.forum.avatarUrl,
                    userId = info.author.uid,
                    userName = info.author.name,
                    userNick = info.author.nick,
                    floor = p?.floor ?: 1,
                    postId = p?.postId ?: info.postId,
                    userAvatar = info.author.avatarUrl
                )
            )
        }
    }

    override fun onPause() {
        super.onPause()
        updateHistory()
        saveLastSeenInfo()
    }

    inner class MyItemDecoration(private val mMargin: Int) : ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val pos = parent.getChildAdapterPosition(view)
            if (pos == RecyclerView.NO_POSITION) return
            val item = postAdapter.snapshot().items[pos]
            val minMarginFloor = if (viewModel.threadConfig.reverse) 0 else 2
            if (item is ThreadViewModel.PostModel.Post && item.post.floor > minMarginFloor) {
                outRect.set(0, mMargin, 0, 0)
            }
        }
    }

    class PostViewHolder(val binding: FragmentThreadPostItemBinding) :
        ViewHolder(binding.root)

    class HeaderViewHolder(val binding: FragmentThreadHeaderBinding) :
        ViewHolder(binding.root)

    class BarViewHolder(val binding: FragmentThreadBarBinding) :
        ViewHolder(binding.root)


    inner class PostAdapter(diffCallback: DiffUtil.ItemCallback<ThreadViewModel.PostModel>) :
        PagingDataAdapter<ThreadViewModel.PostModel, ViewHolder>(
            diffCallback
        ) {

        override fun getItemViewType(position: Int): Int {
            return when (peek(position)) {
                is ThreadViewModel.PostModel.Post -> R.layout.fragment_thread_post_item
                is ThreadViewModel.PostModel.Header -> R.layout.fragment_thread_header
                is ThreadViewModel.PostModel.Bar -> R.layout.fragment_thread_bar
                else -> throw IllegalStateException("unknown item")
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val post = getItem(position) ?: return
            if (holder is PostViewHolder) {
                bindForPost(holder, (post as ThreadViewModel.PostModel.Post).post)
                if (position == mHighlightIdx) {
                    holder.binding.root.isPressed = true
                    mHighlightIdx = -1
                }
            } else if (holder is HeaderViewHolder) {
                bindForHeader(holder)
            } else if (holder is BarViewHolder) {
                bindBar(holder.binding)
                updateBar(holder.binding)
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

            fun showComments(spid: Long) {
                findNavController().navigate(
                    MobileNavigationDirections.showComments(
                        post.tid,
                        post.postId,
                        spid
                    ).setShowOrigin(false)
                )
            }
            holder.binding.root.apply {
                setData(post)
                if (post.commentCount > 0)
                    setOnClickListener {
                        showComments(0)
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
            holder.binding.avatar.displayImageInList(post.user.avatarUrl)
            holder.binding.avatar.setOnClickListener {
                findNavController().navigate(
                    MobileNavigationDirections.showProfile(post.user.uidOrPortrait)
                )
            }
            holder.binding.avatar.setOnLongClickListener {
                it.setSelectedData(SelectedUser(post.user.uidOrPortrait))
                false
            }
            holder.binding.accountName.setOnClickListener {
                findNavController().navigate(
                    MobileNavigationDirections.showProfile(post.user.uidOrPortrait)
                )
            }
            holder.binding.accountName.setOnLongClickListener {
                it.setSelectedData(SelectedUser(post.user.uidOrPortrait))
                false
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
                        val ls = lastString!!
                        val settings = App.settings
                        ls.appendTextAutoLink(
                            content.text,
                            !settings.disableAutoLink,
                            !settings.disableAutoBv
                        )
                    }

                    is Content.LinkContent -> {
                        if (lastString == null) lastString = SpannableStringBuilder()
                        lastString!!.append(
                            content.text.ifEmpty { "[link]" },
                            MyURLSpan(content.link),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                    is Content.UserContent -> {
                        if (lastString == null) lastString = SpannableStringBuilder()
                        lastString!!.append(
                            content.text.ifEmpty { "UID:${content.uidOrPortrait}" },
                            UserSpan(content.uidOrPortrait),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                    is Content.ImageContent -> {
                        addTextView()
                        val imageView =
                            ImageContentBinding.inflate(layoutInflater, contentView, false)
                                .root.apply {
                                    configureImageForContent(content)
                                    displayImageInList(content.previewSrc) {
                                        configureDefaults(context)
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
                                        it.setSelectedData(
                                            content.toPhoto(post)
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
                        videoView.previewImage.displayImageInList(content.previewSrc)
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
                append(post.agreeNum.toSimpleString())
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
                append(post.disagreeNum.toSimpleString())
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
                        UserSpan(comment.user.uidOrPortrait),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    sb.setSpan(
                        StyleSpan(Typeface.BOLD),
                        0,
                        sb.length,
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
                    preview.root.setOnClickListener { showComments(comment.ppid) }
                    preview.root.setOnLongClickListener {
                        it.setSelectedData(comment)
                        false
                    }
                    holder.binding.commentsContent.addView(preview.root)
                }
                if (post.commentCount > 4) {
                    val preview = FragmentThreadCommentPreviewBinding.inflate(layoutInflater)
                    preview.text.text = "查看全部${post.commentCount}条回复"
                    preview.root.setOnClickListener { showComments(0) }
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

                R.layout.fragment_thread_bar -> BarViewHolder(
                    FragmentThreadBarBinding.inflate(
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