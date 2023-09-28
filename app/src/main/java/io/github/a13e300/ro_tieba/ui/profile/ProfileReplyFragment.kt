package io.github.a13e300.ro_tieba.ui.profile

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.databinding.FragmentProfileReplyBinding
import io.github.a13e300.ro_tieba.databinding.FragmentProfileReplyItemBinding
import io.github.a13e300.ro_tieba.models.Reply
import io.github.a13e300.ro_tieba.utils.appendSimpleContent
import io.github.a13e300.ro_tieba.utils.toSimpleString
import kotlinx.coroutines.launch

class ProfileReplyFragment : Fragment() {
    private val viewModel: ProfileViewModel by viewModels({ requireParentFragment() })
    private lateinit var binding: FragmentProfileReplyBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileReplyBinding.inflate(inflater, container, false)
        val replyAdapter = ReplyAdapter(ReplyComparator)
        replyAdapter.addLoadStateListener { state ->
            binding.resultTips.isVisible =
                state.append is LoadState.NotLoading && state.append.endOfPaginationReached && replyAdapter.itemCount == 0
            binding.resultTips.text =
                if (viewModel.postHidden) getString(R.string.user_replies_hidden)
                else getString(R.string.user_replies_empty)
        }
        binding.threadList.adapter = replyAdapter
        binding.threadList.layoutManager = LinearLayoutManager(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.repliesFlow.collect {
                replyAdapter.submitData(it)
            }
        }
        return binding.root
    }

    inner class ReplyAdapter(
        diffCallback: DiffUtil.ItemCallback<Reply>
    ) : PagingDataAdapter<Reply, ReplyAdapter.ReplyViewHolder>(diffCallback) {
        inner class ReplyViewHolder(val binding: FragmentProfileReplyItemBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
            val t = getItem(position) ?: return
            holder.binding.threadTitle.text = t.threadTitle
            holder.binding.threadInfo.text = "${t.time.toSimpleString()} ${t.forumName}吧"
            holder.binding.threadContent.text =
                SpannableStringBuilder().appendSimpleContent(t.content, requireContext())
            holder.binding.root.setOnClickListener {
                val pid = if (t.comment) {
                    t.quota?.pid ?: 0L
                } else t.pid
                if (t.comment) {
                    findNavController().navigate(
                        MobileNavigationDirections.showComments(t.threadId, pid).setSpid(t.pid)
                    )
                } else {
                    findNavController().navigate(
                        MobileNavigationDirections.goToThread(t.threadId).setPid(pid)
                    )
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
            return ReplyViewHolder(
                FragmentProfileReplyItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }
}

object ReplyComparator : DiffUtil.ItemCallback<Reply>() {
    override fun areItemsTheSame(oldItem: Reply, newItem: Reply): Boolean {
        return oldItem.pid == newItem.pid
    }

    override fun areContentsTheSame(oldItem: Reply, newItem: Reply): Boolean {
        return oldItem == newItem
    }
}
