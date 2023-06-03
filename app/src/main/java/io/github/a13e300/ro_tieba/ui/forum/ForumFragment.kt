package io.github.a13e300.ro_tieba.ui.forum

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
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
import com.github.panpf.sketch.displayImage
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.appendSimpleContent
import io.github.a13e300.ro_tieba.databinding.FragmentForumBinding
import io.github.a13e300.ro_tieba.databinding.FragmentForumThreadItemBinding
import io.github.a13e300.ro_tieba.misc.IconSpan
import io.github.a13e300.ro_tieba.models.TiebaThread
import io.github.a13e300.ro_tieba.toSimpleString
import io.github.a13e300.ro_tieba.ui.thread.AVATAR_THUMBNAIL
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ForumFragment : Fragment() {

    private val viewModel: ForumViewModel by viewModels()
    private val args: ForumFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentForumBinding.inflate(inflater, container, false)
        binding.toolbar.title = args.fname
        viewModel.forumName = args.fname
        val threadAdapter = ThreadAdapter(ThreadComparator)
        binding.threadList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = threadAdapter
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val currentUid = App.instance.accountManager.currentAccount.first().uid
                if (viewModel.currentUid == null)
                    viewModel.currentUid = currentUid
                else if (currentUid != viewModel.currentUid) {
                    findNavController().navigateUp()
                    return@repeatOnLifecycle
                }
                viewModel.flow.collect {
                    threadAdapter.submitData(it)
                }
            }
        }
        return binding.root
    }

    inner class ThreadAdapter(diffCallback: DiffUtil.ItemCallback<TiebaThread>) :
        PagingDataAdapter<TiebaThread, ThreadAdapter.ThreadViewHolder>(
            diffCallback
        ) {
        inner class ThreadViewHolder(val binding: FragmentForumThreadItemBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onBindViewHolder(holder: ThreadViewHolder, position: Int) {
            val thread = getItem(position) ?: return
            holder.binding.threadTitle.text = thread.title.ifEmpty { "无标题" }
            holder.binding.threadContent.text = SpannableStringBuilder()
                .appendSimpleContent(thread.content, requireContext())
            holder.binding.threadUserName.text = thread.author.nick.ifEmpty { thread.author.name }
            holder.binding.threadInfo.text = SpannableStringBuilder().apply {
                append(
                    "发帖时间 ",
                    IconSpan(
                        AppCompatResources.getDrawable(
                            requireContext(),
                            R.drawable.ic_time
                        )!!
                    ),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                append(thread.time.toSimpleString())
                append(" ")
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
            holder.binding.threadAvatar.displayImage(
                "$AVATAR_THUMBNAIL/${thread.author.portrait}"
            )
            holder.binding.root.setOnClickListener {
                findNavController().navigate(MobileNavigationDirections.goToThread(thread.tid))
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreadViewHolder {
            return ThreadViewHolder(
                FragmentForumThreadItemBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }
    }
}

object ThreadComparator : DiffUtil.ItemCallback<TiebaThread>() {
    override fun areItemsTheSame(oldItem: TiebaThread, newItem: TiebaThread): Boolean {
        return oldItem.tid == newItem.tid
    }

    override fun areContentsTheSame(oldItem: TiebaThread, newItem: TiebaThread): Boolean {
        return oldItem == newItem
    }
}