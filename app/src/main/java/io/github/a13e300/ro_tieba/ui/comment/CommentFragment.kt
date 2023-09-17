package io.github.a13e300.ro_tieba.ui.comment

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.panpf.sketch.displayImage
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.a13e300.ro_tieba.databinding.FragmentCommentBinding
import io.github.a13e300.ro_tieba.databinding.FragmentCommentItemBinding
import io.github.a13e300.ro_tieba.models.Comment
import io.github.a13e300.ro_tieba.utils.appendSimpleContent
import io.github.a13e300.ro_tieba.utils.toSimpleString
import kotlinx.coroutines.launch

class CommentFragment : BottomSheetDialogFragment() {

    private val viewModel: CommentViewModel by viewModels()
    private val args: CommentFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentCommentBinding.inflate(inflater, container, false)
        // setupToolbar(binding.toolbar)
        viewModel.pid = args.pid
        viewModel.tid = args.tid
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

    class CommentViewHolder(val binding: FragmentCommentItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class CommentAdapter(diffCallback: DiffUtil.ItemCallback<Comment>) :
        PagingDataAdapter<Comment, CommentViewHolder>(
            diffCallback
        ) {

        override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
            val comment = getItem(position) ?: return
            holder.binding.accountName.text =
                comment.user.nick.ifEmpty { comment.user.name }.ifEmpty { "[${comment.user.uid}]" }
            holder.binding.commentContent.text =
                SpannableStringBuilder().appendSimpleContent(
                    comment.content,
                    requireContext(),
                    useUrlSpan = true
                )
            holder.binding.avatar.displayImage(comment.user.avatarUrl)
            holder.binding.description.text =
                comment.time.toSimpleString()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
            return CommentViewHolder(
                FragmentCommentItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }
    }
}

object CommentComparator : DiffUtil.ItemCallback<Comment>() {
    override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
        return oldItem.ppid == newItem.ppid
    }

    override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
        return oldItem == newItem
    }
}