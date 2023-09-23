package io.github.a13e300.ro_tieba.ui.comment

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.github.panpf.sketch.displayImage
import io.github.a13e300.ro_tieba.BaseFragment
import io.github.a13e300.ro_tieba.Emotions
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.databinding.FragmentCommentBinding
import io.github.a13e300.ro_tieba.databinding.FragmentCommentItemBinding
import io.github.a13e300.ro_tieba.databinding.FragmentThreadPostItemBinding
import io.github.a13e300.ro_tieba.databinding.ImageContentBinding
import io.github.a13e300.ro_tieba.misc.EmojiSpan
import io.github.a13e300.ro_tieba.misc.IconSpan
import io.github.a13e300.ro_tieba.misc.MyURLSpan
import io.github.a13e300.ro_tieba.misc.PlaceHolderDrawable
import io.github.a13e300.ro_tieba.models.Comment
import io.github.a13e300.ro_tieba.models.Content
import io.github.a13e300.ro_tieba.models.Post
import io.github.a13e300.ro_tieba.ui.DetailDialogFragment
import io.github.a13e300.ro_tieba.ui.photo.Photo
import io.github.a13e300.ro_tieba.ui.photo.PhotoViewModel
import io.github.a13e300.ro_tieba.ui.toDetail
import io.github.a13e300.ro_tieba.utils.appendSimpleContent
import io.github.a13e300.ro_tieba.utils.appendUserInfo
import io.github.a13e300.ro_tieba.utils.setSelectedData
import io.github.a13e300.ro_tieba.utils.toSimpleString
import io.github.a13e300.ro_tieba.view.ContentTextView
import kotlinx.coroutines.launch

class CommentFragment : BaseFragment() {

    private val viewModel: CommentViewModel by viewModels()
    private val photoViewModel: PhotoViewModel by viewModels({ findNavController().currentBackStackEntry!! })
    private val args: CommentFragmentArgs by navArgs()
    private lateinit var binding: FragmentCommentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCommentBinding.inflate(inflater, container, false)
        setupToolbar(binding.toolbar)
        viewModel.pid = args.pid
        viewModel.tid = args.tid
        viewModel.floor.observe(viewLifecycleOwner) {
            binding.toolbar.title = "$it 楼的评论"
        }
        viewModel.commentCount.observe(viewLifecycleOwner) {
            binding.toolbar.subtitle = "共 $it 条"
        }
        val commentAdapter = CommentAdapter(CommentComparator)
        binding.list.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = commentAdapter
        }
        lifecycleScope.launch {
            viewModel.flow.collect {
                commentAdapter.submitData(it)
            }
        }
        return binding.root
    }

    class PostViewHolder(val binding: FragmentThreadPostItemBinding) :
        ViewHolder(binding.root)

    class CommentViewHolder(val binding: FragmentCommentItemBinding) :
        ViewHolder(binding.root)

    inner class CommentAdapter(diffCallback: DiffUtil.ItemCallback<CommentItem>) :
        PagingDataAdapter<CommentItem, ViewHolder>(
            diffCallback
        ) {

        private fun bindForComment(holder: CommentViewHolder, comment: Comment) {
            val context = requireContext()
            holder.binding.accountName.text =
                SpannableStringBuilder().append(comment.user.showName).appendUserInfo(
                    comment.user, viewModel.threadAuthorUid == comment.user.uid,
                    context,
                    showLevel = true
                )
            holder.binding.commentContent.text =
                SpannableStringBuilder().appendSimpleContent(
                    comment.content,
                    requireContext(),
                    useUrlSpan = true
                )
            holder.binding.avatar.displayImage(comment.user.avatarUrl)
            holder.binding.avatar.setOnClickListener {
                findNavController().navigate(MobileNavigationDirections.showProfile(comment.user.uidOrPortrait))
            }
            holder.binding.description.text = SpannableStringBuilder()
                .append(
                    "时间 ",
                    IconSpan(
                        AppCompatResources.getDrawable(
                            requireContext(),
                            R.drawable.ic_time
                        )!!
                    ),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                ).append(comment.time.toSimpleString())
            holder.binding.description.setOnClickListener {
                val (ks, vs) = comment.toDetail()
                DetailDialogFragment.newInstance(ks, vs).show(childFragmentManager, "detail")
            }
            holder.binding.root.setData(comment)
        }

        private fun bindForPost(holder: PostViewHolder, post: Post) {
            val context = requireContext()
            holder.binding.accountName.text = post.user.showName
            holder.binding.accountInfo.text = SpannableStringBuilder().appendUserInfo(
                post.user, viewModel.threadAuthorUid == post.user.uid,
                context,
                showLevel = true
            )
            val contentView = holder.binding.content
            val fontSize = context.resources.getDimensionPixelSize(R.dimen.content_text_size)
            contentView.removeAllViews()
            var lastString: SpannableStringBuilder? = null
            holder.binding.avatar.displayImage(post.user.avatarUrl)
            holder.binding.avatar.setOnClickListener {
                findNavController().navigate(
                    MobileNavigationDirections.showProfile(post.user.uidOrPortrait)
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
                                        val photos =
                                            post.content.filterIsInstance<Content.ImageContent>()
                                                .map { c -> Photo(c.src, c.order, post) }
                                        val idx = content.order
                                        photoViewModel.photos = photos
                                        photoViewModel.currentIndex.value = idx
                                        findNavController().navigate(
                                            MobileNavigationDirections.viewPhotos(),
                                            // navigatorExtras = FragmentNavigatorExtras(it to name)
                                        )
                                    }
                                    setOnLongClickListener {
                                        it.setSelectedData(
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
            holder.binding.root.setData(post)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position) ?: return
            when (item) {
                is CommentItem.Comment -> bindForComment(holder as CommentViewHolder, item.comment)
                is CommentItem.Post -> bindForPost(holder as PostViewHolder, item.post)
            }
        }

        override fun getItemViewType(position: Int): Int {
            return when (getItem(position)) {
                is CommentItem.Comment -> R.layout.fragment_comment_item
                is CommentItem.Post -> R.layout.fragment_thread_post_item
                else -> throw IllegalArgumentException("unknown item type")
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return when (viewType) {
                R.layout.fragment_comment_item -> CommentViewHolder(
                    FragmentCommentItemBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    ).apply { registerForContextMenu(root) }
                )

                R.layout.fragment_thread_post_item -> PostViewHolder(
                    FragmentThreadPostItemBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    ).apply { registerForContextMenu(root) }
                )

                else -> throw IllegalArgumentException("unknown type $viewType")
            }
        }
    }
}

object CommentComparator : DiffUtil.ItemCallback<CommentItem>() {
    override fun areItemsTheSame(oldItem: CommentItem, newItem: CommentItem): Boolean {
        if (oldItem is CommentItem.Comment && newItem is CommentItem.Comment) return oldItem.comment.ppid == newItem.comment.ppid
        else if (oldItem is CommentItem.Post && newItem is CommentItem.Post) return oldItem.post.postId == newItem.post.postId
        return false
    }

    override fun areContentsTheSame(oldItem: CommentItem, newItem: CommentItem): Boolean {
        return oldItem == newItem
    }
}