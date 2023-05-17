package io.github.a13e300.ro_tieba.ui.thread

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.github.a13e300.ro_tieba.databinding.FragmentThreadPostItemBinding

class PostAdapter(diffCallback: DiffUtil.ItemCallback<Post>) :
    PagingDataAdapter<Post, PostAdapter.PostViewHolder>(
        diffCallback
    ) {
    inner class PostViewHolder(val binding: FragmentThreadPostItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position) ?: return
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
            )
        )
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
