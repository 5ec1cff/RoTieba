package io.github.a13e300.ro_tieba.ui.forum

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.databinding.FragmentForumBinding
import io.github.a13e300.ro_tieba.databinding.FragmentForumThreadItemBinding
import io.github.a13e300.ro_tieba.ui.thread.AVATAR_THUMBNAIL
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
            viewModel.flow.collect {
                threadAdapter.submitData(it)
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
            holder.binding.threadTitle.text = thread.title
            holder.binding.threadContent.text = thread.content
            holder.binding.threadUserName.text = thread.author.nick.ifEmpty { thread.author.name }
            Glide.with(holder.binding.root)
                .load("$AVATAR_THUMBNAIL/${thread.author.portrait}")
                .into(holder.binding.threadAvatar)
            holder.binding.root.setOnClickListener {
                findNavController().navigate(MobileNavigationDirections.goToThread(thread.tid.toString()))
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