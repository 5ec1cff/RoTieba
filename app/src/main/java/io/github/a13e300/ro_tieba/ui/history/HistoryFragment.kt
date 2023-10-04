package io.github.a13e300.ro_tieba.ui.history

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.github.panpf.sketch.displayImage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.BaseFragment
import io.github.a13e300.ro_tieba.DEFAULT_FORUM_AVATAR
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.databinding.FragmentHistoryBinding
import io.github.a13e300.ro_tieba.databinding.FragmentHistoryForumItemBinding
import io.github.a13e300.ro_tieba.databinding.FragmentHistoryPostItemBinding
import io.github.a13e300.ro_tieba.databinding.FragmentHistoryUserItemBinding
import io.github.a13e300.ro_tieba.db.EntryType
import io.github.a13e300.ro_tieba.db.HistoryEntry
import io.github.a13e300.ro_tieba.history.HistoryManager
import io.github.a13e300.ro_tieba.ui.DetailDialogFragment
import io.github.a13e300.ro_tieba.ui.toDetail
import io.github.a13e300.ro_tieba.utils.forceShowIcon
import io.github.a13e300.ro_tieba.utils.toSimpleString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class HistoryFragment : BaseFragment() {
    private val viewModel by viewModels<HistoryViewModel>()
    private var mPendingScrollToTop = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentHistoryBinding.inflate(inflater, container, false)
        setupToolbar(binding.toolbar)
        binding.toolbar.setOnMenuItemClickListener {
            return@setOnMenuItemClickListener when (it.itemId) {
                R.id.remove_all -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("确定删除所有历史记录？")
                        .setPositiveButton("确定") { _, _ ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                App.instance.db.historyDao().removeAll()
                            }
                        }
                        .setNegativeButton("取消", null)
                        .show()
                    true
                }

                else -> false
            }
        }
        val adapter = HistoryAdapter()
        binding.list.adapter = adapter
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.flow.collect {
                adapter.submitData(it)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            App.instance.db.historyDao().count().collect {
                binding.toolbar.subtitle = "$it / ${HistoryManager.MAX_HISTORY_ENTRY_COUNT}"
            }
        }
        adapter.addLoadStateListener { state ->
            val empty =
                state.append is LoadState.NotLoading && state.append.endOfPaginationReached && adapter.itemCount == 0
            binding.resultTips.isVisible = empty
            binding.toolbar.menu.findItem(R.id.remove_all).isVisible = !empty
            if (mPendingScrollToTop && state.prepend is LoadState.NotLoading) {
                val first = if (adapter.itemCount != 0) adapter.peek(0) else null
                if (first != null) {
                    binding.list.scrollToPosition(0)
                    mPendingScrollToTop = false
                }
            }
        }
        binding.toolbar.setOnClickListener {
            if (adapter.itemCount == 0) return@setOnClickListener
            val first = adapter.requestItem(0)
            if (first != null && !mPendingScrollToTop) {
                binding.list.scrollToPosition(0)
            } else {
                mPendingScrollToTop = true
            }
        }
        return binding.root
    }

    class PostViewHolder(val binding: FragmentHistoryPostItemBinding) : ViewHolder(binding.root)
    class ForumViewHolder(val binding: FragmentHistoryForumItemBinding) : ViewHolder(binding.root)
    class UserViewHolder(val binding: FragmentHistoryUserItemBinding) : ViewHolder(binding.root)

    @SuppressLint("SetTextI18n")
    inner class HistoryAdapter : PagingDataAdapter<HistoryEntry, ViewHolder>(HistoryComparator) {
        fun requestItem(pos: Int) = getItem(pos)
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position) ?: return
            when (holder) {
                is PostViewHolder -> {
                    holder.binding.threadTitle.text = item.title
                    holder.binding.threadInfo.text =
                        "${Date(item.time).toSimpleString()} 看到第 ${item.floor} 楼"
                    holder.binding.threadUserName.text = item.userNick.ifEmpty { item.userName }
                    holder.binding.threadAvatar.displayImage(item.userAvatar)
                    holder.binding.threadForum.text = "${item.forumName}吧"
                    holder.binding.forumAvatar.displayImage(item.forumAvatar.ifEmpty { DEFAULT_FORUM_AVATAR })
                    holder.binding.forumCard.setOnClickListener {
                        findNavController().navigate(MobileNavigationDirections.goToForum(item.forumName))
                    }
                    holder.binding.root.setOnClickListener {
                        findNavController().navigate(
                            MobileNavigationDirections.goToThread(item.id.toLong())
                                .setPid(item.postId)
                        )
                    }
                    holder.binding.threadAvatar.setOnClickListener {
                        findNavController().navigate(MobileNavigationDirections.showProfile(item.userId.toString()))
                    }
                }

                is ForumViewHolder -> {
                    holder.binding.forumName.text = item.forumName
                    holder.binding.info.text = "${Date(item.time).toSimpleString()} 进入了贴吧"
                    holder.binding.forumAvatar.displayImage(item.forumAvatar.ifEmpty { DEFAULT_FORUM_AVATAR })
                    holder.binding.root.setOnClickListener {
                        findNavController().navigate(MobileNavigationDirections.goToForum(item.forumName))
                    }
                }

                is UserViewHolder -> {
                    holder.binding.userName.text = item.userNick.ifEmpty { item.userName }
                    holder.binding.userAvatar.displayImage(item.userAvatar)
                    holder.binding.info.text = "${Date(item.time).toSimpleString()} 访问了用户"
                    holder.binding.root.setOnClickListener {
                        findNavController().navigate(MobileNavigationDirections.showProfile(item.id))
                    }
                }
            }
            holder.itemView.setOnLongClickListener {
                it.setOnCreateContextMenuListener { contextMenu, _, _ ->
                    MenuInflater(requireContext()).inflate(R.menu.history_item_menu, contextMenu)
                    contextMenu.forceShowIcon()
                    contextMenu.findItem(R.id.delete).setOnMenuItemClickListener {
                        lifecycleScope.launch {
                            App.instance.historyManager.deleteHistory(item)
                        }
                        true
                    }
                    contextMenu.findItem(R.id.detail).setOnMenuItemClickListener {
                        val (ks, kv) = item.toDetail()
                        DetailDialogFragment.newInstance(ks, kv)
                            .show(childFragmentManager, "detail")
                        true
                    }
                }
                false
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return when (viewType) {
                R.layout.fragment_history_post_item -> PostViewHolder(
                    FragmentHistoryPostItemBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                )

                R.layout.fragment_history_forum_item -> ForumViewHolder(
                    FragmentHistoryForumItemBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                )

                R.layout.fragment_history_user_item -> UserViewHolder(
                    FragmentHistoryUserItemBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                )

                else -> throw IllegalArgumentException("")
            }.also {
                registerForContextMenu(it.itemView)
            }
        }

        override fun getItemViewType(position: Int): Int {
            val item = peek(position)
            return when (item!!.type) {
                EntryType.THREAD -> R.layout.fragment_history_post_item
                EntryType.FORUM -> R.layout.fragment_history_forum_item
                EntryType.USER -> R.layout.fragment_history_user_item
            }
        }

    }
}

object HistoryComparator : DiffUtil.ItemCallback<HistoryEntry>() {
    override fun areItemsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry): Boolean {
        return oldItem.id == newItem.id && oldItem.type == newItem.type
    }

    override fun areContentsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry): Boolean {
        return oldItem == newItem
    }
}
