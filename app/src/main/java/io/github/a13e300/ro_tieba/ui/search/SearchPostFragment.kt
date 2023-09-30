package io.github.a13e300.ro_tieba.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.panpf.sketch.displayImage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.api.web.SearchFilter
import io.github.a13e300.ro_tieba.api.web.SearchOrder
import io.github.a13e300.ro_tieba.databinding.FragmentSearchPostBinding
import io.github.a13e300.ro_tieba.databinding.FragmentSearchPostItemBinding
import io.github.a13e300.ro_tieba.databinding.FragmentSearchPostLoadStateBinding
import io.github.a13e300.ro_tieba.models.PostId
import io.github.a13e300.ro_tieba.models.SearchedPost
import io.github.a13e300.ro_tieba.ui.DetailDialogFragment
import io.github.a13e300.ro_tieba.ui.toDetail
import io.github.a13e300.ro_tieba.utils.replaceEm
import io.github.a13e300.ro_tieba.utils.toSimpleString
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.reduce
import kotlinx.coroutines.launch


class SearchPostFragment : Fragment() {
    private val viewModel: SearchViewModel by viewModels({ requireParentFragment() })
    private lateinit var binding: FragmentSearchPostBinding
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchPostBinding.inflate(inflater, container, false)
        postAdapter = PostAdapter(SearchedPostComparator)
        binding.postList.apply {
            adapter = postAdapter.withLoadStateHeader(
                StateAdapter()
            )
            layoutManager = LinearLayoutManager(requireContext())
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.flow.collect { data ->
                postAdapter.submitData(data)
                postAdapter.submitData(PagingData.empty())
            }
        }
        binding.filterTypeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val newValue = when (checkedId) {
                    R.id.filter_all -> SearchFilter.ALL
                    R.id.filter_thread -> SearchFilter.THREAD
                    else -> error("invalid id!")
                }
                if (newValue != viewModel.searchPostFilter) {
                    viewModel.searchPostFilter = newValue
                    postAdapter.refresh()
                }
            }
        }
        binding.orderButton.apply {
            viewModel.searchPostOrder.observe(viewLifecycleOwner) {
                text = when (it!!) {
                    SearchOrder.NEW -> getString(R.string.search_post_order_new)
                    SearchOrder.OLD -> getString(R.string.search_post_order_old)
                    SearchOrder.RELEVANT -> getString(R.string.search_post_order_relevant)
                }
            }
            setOnClickListener { btn ->
                val popup = PopupMenu(requireContext(), btn)
                popup.menuInflater.inflate(R.menu.search_order_menu, popup.menu)
                popup.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.search_order_new -> {
                            viewModel.searchPostOrder.value = SearchOrder.NEW
                        }

                        R.id.search_order_old -> {
                            viewModel.searchPostOrder.value = SearchOrder.OLD
                        }

                        R.id.search_order_relevant -> {
                            viewModel.searchPostOrder.value = SearchOrder.RELEVANT
                        }
                    }
                    postAdapter.refresh()
                    true
                }
                popup.show()
            }
        }
        postAdapter.addLoadStateListener { state ->
            (state.refresh as? LoadState.Error)?.error?.let {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.error_dialog_title)
                    .setMessage(it.message)
                    .show()
            }
            binding.resultTips.isVisible =
                state.append is LoadState.NotLoading && state.append.endOfPaginationReached && postAdapter.itemCount == 0
        }
        viewLifecycleOwner.lifecycleScope.launch {
            postAdapter.loadStateFlow
                .distinctUntilChangedBy { it.refresh }
                .map {
                    it.refresh
                }
                .reduce { prev, current ->
                    if (prev is LoadState.Loading && current is LoadState.NotLoading) {
                        binding.postList.scrollToPosition(0)
                    }
                    current
                }
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.searchPostEvent.observe(viewLifecycleOwner) { event ->
            event.handle {
                viewModel.postKeyWord = it
                postAdapter.refresh()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.searchPostEvent.removeObservers(viewLifecycleOwner)
    }

    class LoadStateHolder(val binding: FragmentSearchPostLoadStateBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class StateAdapter : LoadStateAdapter<LoadStateHolder>() {
        override fun onBindViewHolder(holder: LoadStateHolder, loadState: LoadState) {
            if (loadState is LoadState.Error) {
                holder.binding.errorMessage.text = loadState.error.message
                holder.binding.retryButton.isGone = false
            } else {
                holder.binding.retryButton.isGone = true
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadStateHolder =
            LoadStateHolder(
                FragmentSearchPostLoadStateBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )

        private fun listEmpty(loadState: LoadState) =
            loadState is LoadState.NotLoading && loadState.endOfPaginationReached && postAdapter.itemCount == 0

        override fun displayLoadStateAsItem(loadState: LoadState): Boolean {
            return super.displayLoadStateAsItem(loadState) || listEmpty(loadState)
        }

    }

    class ViewHolder(val binding: FragmentSearchPostItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class PostAdapter(diffCallback: DiffUtil.ItemCallback<SearchedPost>) :
        PagingDataAdapter<SearchedPost, ViewHolder>(
            diffCallback
        ) {
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position) ?: return
            holder.binding.threadContent.text =
                if (viewModel.searchAtForum) item.content.replaceEm(requireContext()) else item.content
            holder.binding.threadUserName.text = item.user.showName
            holder.binding.threadTitle.text =
                if (viewModel.searchAtForum) item.title.replaceEm(requireContext()) else item.title
            if (!viewModel.searchAtForum) {
                holder.binding.threadForum.text = "${item.forum.name}吧"
                holder.binding.forumAvatar.displayImage(item.forum.avatarUrl)
                holder.binding.forumCard.setOnClickListener {
                    findNavController().navigate(MobileNavigationDirections.goToForum(item.forum.name))
                }
            }
            holder.binding.forumCard.isGone = viewModel.searchAtForum
            holder.binding.threadInfo.text = item.time.toSimpleString()
            val avatar = item.user.avatarUrl
            if (avatar.isNotEmpty())
                holder.binding.threadAvatar.displayImage(avatar)
            holder.binding.threadAvatar.isGone = avatar.isEmpty()
            val uid = item.user.uid
            holder.binding.threadAvatar.setOnClickListener(if (uid == 0L) null else View.OnClickListener {
                findNavController().navigate(MobileNavigationDirections.showProfile(uid.toString()))
            })
            holder.binding.root.setOnClickListener {
                val id = item.id
                findNavController().navigate(
                    when (id) {
                        is PostId.Comment -> MobileNavigationDirections.showComments(
                            id.tid,
                            id.pid,
                            id.spid
                        )

                        is PostId.Post -> MobileNavigationDirections.goToThread(id.tid)
                            .setPid(id.pid)

                        is PostId.Thread -> MobileNavigationDirections.goToThread(id.tid)
                    }
                )
            }
            holder.binding.threadInfo.setOnClickListener {
                val (ks, vs) = item.toDetail()
                DetailDialogFragment.newInstance(ks, vs).show(childFragmentManager, "detail")
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder {
            return ViewHolder(
                FragmentSearchPostItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }
}

object SearchedPostComparator : DiffUtil.ItemCallback<SearchedPost>() {
    override fun areItemsTheSame(oldItem: SearchedPost, newItem: SearchedPost): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SearchedPost, newItem: SearchedPost): Boolean {
        return oldItem == newItem
    }
}