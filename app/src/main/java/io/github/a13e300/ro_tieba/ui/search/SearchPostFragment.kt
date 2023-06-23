package io.github.a13e300.ro_tieba.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.panpf.sketch.displayImage
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.api.web.SearchFilter
import io.github.a13e300.ro_tieba.api.web.SearchOrder
import io.github.a13e300.ro_tieba.databinding.FragmentSearchPostBinding
import io.github.a13e300.ro_tieba.databinding.FragmentSearchPostItemBinding
import io.github.a13e300.ro_tieba.models.SearchedPost
import io.github.a13e300.ro_tieba.toSimpleString
import io.github.a13e300.ro_tieba.ui.DetailDialogFragment
import io.github.a13e300.ro_tieba.ui.toDetail
import kotlinx.coroutines.launch


class SearchPostFragment : Fragment() {
    private val viewModel: SearchViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSearchPostBinding.inflate(inflater, container, false)
        val postAdapter = PostAdapter(SearchedPostComparator)
        binding.postList.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        viewModel.currentKeyword.observe(viewLifecycleOwner) {
            if (it != viewModel.searchPostKeyWord) {
                postAdapter.refresh()
                binding.postList.scrollToPosition(0)
            }
        }
        lifecycleScope.launch {
            viewModel.flow.collect { data ->
                postAdapter.submitData(data)
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
        return binding.root
    }

    class ViewHolder(val binding: FragmentSearchPostItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class PostAdapter(diffCallback: DiffUtil.ItemCallback<SearchedPost>) :
        PagingDataAdapter<SearchedPost, ViewHolder>(
            diffCallback
        ) {
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position) ?: return
            holder.binding.threadContent.text = item.content
            holder.binding.threadUserName.text = item.post.user.showName
            holder.binding.threadTitle.text = item.title
            holder.binding.threadInfo.text = "${item.forum}吧 ${item.post.time.toSimpleString()}"
            holder.binding.threadAvatar.displayImage(item.post.user.avatarUrl)
            holder.binding.root.setOnClickListener {
                findNavController().navigate(
                    MobileNavigationDirections.goToThread(item.post.tid).setPid(item.post.postId)
                )
            }
            holder.binding.threadInfo.setOnClickListener {
                findNavController().navigate(MobileNavigationDirections.goToForum(item.forum))
            }
            holder.binding.threadInfo.setOnLongClickListener {
                val (ks, vs) = item.toDetail()
                DetailDialogFragment.newInstance(ks, vs).show(childFragmentManager, "detail")
                true
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
        return oldItem.post.postId == newItem.post.postId
    }

    override fun areContentsTheSame(oldItem: SearchedPost, newItem: SearchedPost): Boolean {
        return oldItem == newItem
    }
}