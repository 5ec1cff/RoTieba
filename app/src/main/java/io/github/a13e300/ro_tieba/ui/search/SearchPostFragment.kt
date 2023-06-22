package io.github.a13e300.ro_tieba.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import io.github.a13e300.ro_tieba.databinding.FragmentSearchPostBinding
import io.github.a13e300.ro_tieba.databinding.FragmentSearchPostItemBinding
import io.github.a13e300.ro_tieba.models.SearchedPost
import io.github.a13e300.ro_tieba.toSimpleString
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