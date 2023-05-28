package io.github.a13e300.ro_tieba.ui.thread

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.text.style.URLSpan
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.Emotions
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.databinding.FragmentThreadBinding
import io.github.a13e300.ro_tieba.databinding.FragmentThreadPostItemBinding
import io.github.a13e300.ro_tieba.forceShowIcon
import io.github.a13e300.ro_tieba.toSimpleString
import io.github.a13e300.ro_tieba.view.ItemView
import io.github.a13e300.ro_tieba.view.MyLinkMovementMethod
import io.github.a13e300.ro_tieba.view.PbContentTextView
import io.github.a13e300.ro_tieba.view.SelectedLink
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ThreadFragment : Fragment() {

    private val viewModel: ThreadViewModel by viewModels()
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
        binding.list.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = postAdapter
        }
        viewModel.threadTitle.observe(viewLifecycleOwner) {
            binding.toolbar.title = it
        }
        binding.toolbar.addMenuProvider(object : MenuProvider {
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.refresh) {
                    binding.list.scrollToPosition(0)
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
                            is Post.TextContent -> it.text
                            is Post.ImageContent -> "[${it.src}]"
                            is Post.LinkContent -> "[${it.text}](${it.link})"
                            is Post.EmojiContent -> Emotions.emotionMap.get(it.id)?.name ?: it.id
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

    class PostViewHolder(val binding: FragmentThreadPostItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class PostAdapter(diffCallback: DiffUtil.ItemCallback<Post>) :
        PagingDataAdapter<Post, PostViewHolder>(
            diffCallback
        ) {

        override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
            val post = getItem(position) ?: return
            holder.binding.root.setData(post)
            holder.binding.accountName.text =
                post.user.nick.ifEmpty { post.user.name }.ifEmpty { "[${post.user.uid}]" }
            val contentView = holder.binding.content
            contentView.removeAllViews()
            var lastString: SpannableStringBuilder? = null
            val context = holder.binding.root.context
            Glide.with(context).load("$AVATAR_THUMBNAIL/${post.user.portrait}")
                .into(holder.binding.avatar)
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
                })
                lastString = null
            }
            for (content in post.content) {
                when (content) {
                    is Post.TextContent -> {
                        if (lastString == null) lastString = SpannableStringBuilder()
                        lastString!!.append(content.text)
                    }

                    is Post.LinkContent -> {
                        if (lastString == null) lastString = SpannableStringBuilder()
                        lastString!!.append(
                            content.text.ifEmpty { "[link]" },
                            URLSpan(content.link),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }

                    is Post.ImageContent -> {
                        addTextView()
                        val imageView = AppCompatImageView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            adjustViewBounds = true
                            Glide.with(contentView).load(content.previewSrc)
                                .override(content.width, content.height).into(this)
                        }
                        contentView.addView(imageView)
                    }

                    is Post.EmojiContent -> {
                        if (lastString == null) lastString = SpannableStringBuilder()
                        val emoji = Emotions.emotionMap.get(content.id)
                        if (emoji == null) {
                            lastString!!.append("[${content.id}]")
                        } else {
                            val drawable =
                                AppCompatResources.getDrawable(requireContext(), emoji.resource)!!
                                    .apply {
                                        setBounds(0, 0, 50, 50)
                                    }
                            lastString!!.append(
                                emoji.name,
                                ImageSpan(drawable),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                    }

                    else -> {}
                }
            }
            addTextView()
            holder.binding.floorNum.text =
                "${post.floor}楼·${post.time.toSimpleString()}·${post.user.location}"
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