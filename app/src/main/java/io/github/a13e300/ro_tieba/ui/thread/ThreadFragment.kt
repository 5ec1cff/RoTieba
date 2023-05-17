package io.github.a13e300.ro_tieba.ui.thread

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.databinding.FragmentThreadBinding
import io.github.a13e300.ro_tieba.databinding.FragmentThreadPostItemBinding
import io.github.a13e300.ro_tieba.forceShowIcon
import io.github.a13e300.ro_tieba.view.ItemView
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
            viewModel.flow.collect { data ->
                postAdapter.submitData(data)
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
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val post = (item.menuInfo as? ItemView.ContextMenuInfo)?.data as? Post
        if (post != null) {
            when (item.itemId) {
                R.id.copy_post_content -> {
                    val cm = requireContext().getSystemService(ClipboardManager::class.java)
                    cm.setPrimaryClip(ClipData.newPlainText("", post.content.joinToString("") {
                        when (it) {
                            is Post.TextContent -> it.text
                            is Post.ImageContent -> "[${it.src}]"
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
            var lastString: StringBuilder? = null
            val context = holder.binding.root.context
            Glide.with(context).load("$AVATAR_THUMBNAIL/${post.user.portrait}")
                .into(holder.binding.avatar)
            fun addTextView() {
                if (lastString == null) return
                contentView.addView(AppCompatTextView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    text = lastString.toString()
                })
                lastString = null
            }
            for (content in post.content) {
                when (content) {
                    is Post.TextContent -> {
                        if (lastString == null) lastString = StringBuilder()
                        lastString!!.append(content.text)
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
                }
            }
            addTextView()
            holder.binding.floorNum.text = "${post.floor}楼"
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