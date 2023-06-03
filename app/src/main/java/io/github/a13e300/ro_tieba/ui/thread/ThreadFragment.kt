package io.github.a13e300.ro_tieba.ui.thread

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.URLSpan
import android.util.TypedValue
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
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
import com.github.panpf.sketch.displayImage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.Emotions
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.appendSimpleContent
import io.github.a13e300.ro_tieba.databinding.FragmentThreadBinding
import io.github.a13e300.ro_tieba.databinding.FragmentThreadPostItemBinding
import io.github.a13e300.ro_tieba.databinding.ImageContentBinding
import io.github.a13e300.ro_tieba.databinding.ThreadListFooterBinding
import io.github.a13e300.ro_tieba.forceShowIcon
import io.github.a13e300.ro_tieba.misc.EmojiSpan
import io.github.a13e300.ro_tieba.misc.IconSpan
import io.github.a13e300.ro_tieba.misc.PlaceHolderDrawable
import io.github.a13e300.ro_tieba.models.Content
import io.github.a13e300.ro_tieba.models.Post
import io.github.a13e300.ro_tieba.toSimpleString
import io.github.a13e300.ro_tieba.ui.photo.PhotoViewModel
import io.github.a13e300.ro_tieba.utils.appendUser
import io.github.a13e300.ro_tieba.view.ItemView
import io.github.a13e300.ro_tieba.view.MyLinkMovementMethod
import io.github.a13e300.ro_tieba.view.PbContentTextView
import io.github.a13e300.ro_tieba.view.SelectedLink
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ThreadFragment : Fragment() {

    private val viewModel: ThreadViewModel by viewModels()
    private val photoViewModel: PhotoViewModel by viewModels({ findNavController().currentBackStackEntry!! })
    private val args: ThreadFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentThreadBinding.inflate(inflater, container, false)
        if (viewModel.threadConfig.value == null) {
            viewModel.threadConfig.value = ThreadConfig(args.tid.toLong())
            Logger.d("update thread config")
        }
        val postAdapter = PostAdapter(PostComparator)
        postAdapter.addLoadStateListener { state ->
            (state.refresh as? LoadState.Error)?.error?.let {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("error")
                    .setMessage(it.message)
                    .show()
            }
        }
        binding.list.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = postAdapter.withLoadStateFooter(FooterAdapter())
        }
        viewModel.threadInfo.observe(viewLifecycleOwner) {
            binding.toolbar.title = it.title
        }
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
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as? ItemView.ContextMenuInfo
        val post = info?.data as? Post
        val selected = info?.selectedData
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
                    cm.setPrimaryClip(
                        ClipData.newPlainText(
                            "",
                            "https://tieba.baidu.com/p/${post.tid}?p=${post.postId}"
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
            }
        }
        return super.onContextItemSelected(item)
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
            holder.binding.accountName.text = SpannableStringBuilder().appendUser(
                post.user, post.user.uid == viewModel.threadInfo.value?.author?.uid,
                context
            )
            val contentView = holder.binding.content
            val fontSize = context.resources.getDimensionPixelSize(R.dimen.content_text_size)
            val emojiSize = (fontSize * 1.2).toInt()
            contentView.removeAllViews()
            var lastString: SpannableStringBuilder? = null
            holder.binding.avatar.displayImage("$AVATAR_THUMBNAIL/${post.user.portrait}")
            // TODO: refactor this to use single TextView
            fun addTextView() {
                if (lastString == null) return
                contentView.addView(PbContentTextView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    text = lastString
                    movementMethod = MyLinkMovementMethod
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
                            URLSpan(content.link),
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
                                        photoViewModel.photos = viewModel.photos.values.toList()
                                        val idx =
                                            viewModel.photos.keys.indexOf(post.floor to content.order)
                                        photoViewModel.currentIndex.value = idx
                                        findNavController().navigate(MobileNavigationDirections.viewPhotos())
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
                val sb = SpannableStringBuilder()
                post.comments.forEach {
                    sb.appendUser(
                        it.user,
                        it.user.uid == viewModel.threadInfo.value?.author?.uid,
                        requireContext()
                    )
                    sb.append(": ")
                    sb.appendSimpleContent(it.content, requireContext())
                    sb.append("\n")
                }
                sb.append("共${post.commentCount}条回复")
                holder.binding.commentsContent.text = sb
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