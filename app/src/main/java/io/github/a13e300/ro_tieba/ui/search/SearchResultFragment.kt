package io.github.a13e300.ro_tieba.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.panpf.sketch.displayImage
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.databinding.FragmentSearchResultBarItemBinding
import io.github.a13e300.ro_tieba.databinding.FragmentSearchResultBinding

class SearchResultFragment : Fragment() {
    private val viewModel: SearchViewModel by viewModels({ requireParentFragment() })
    private val myAdapter = Adapter()
    private lateinit var binding: FragmentSearchResultBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchResultBinding.inflate(inflater, container, false)
        binding.resultList.apply {
            adapter = myAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        viewModel.barLoadState.observe(viewLifecycleOwner) {
            when (it) {
                LoadState.FETCHED -> {
                    binding.resultList.scrollToPosition(0)
                    myAdapter.notifyDataSetChanged()
                    updateData()
                    viewModel.barLoadState.value = LoadState.LOADED
                }

                else -> {}
            }
        }
        updateData()
        return binding.root
    }

    private fun updateData() {
        viewModel.searchedForums.also { frs ->
            when (frs) {
                is SearchResult.Result -> {
                    if (frs.data.isEmpty()) {
                        binding.resultList.visibility = View.GONE
                        binding.resultTips.visibility = View.VISIBLE
                        binding.resultTips.setText(R.string.no_result_tips)
                    } else {
                        binding.resultList.visibility = View.VISIBLE
                        binding.resultTips.visibility = View.GONE
                    }
                }

                is SearchResult.Error -> {
                    binding.resultList.visibility = View.GONE
                    binding.resultTips.visibility = View.VISIBLE
                    binding.resultTips.text = frs.error.message
                }
            }
        }
    }

    class ViewHolder(val binding: FragmentSearchResultBarItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class Adapter : RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
            FragmentSearchResultBarItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

        override fun getItemCount(): Int =
            (viewModel.searchedForums as? SearchResult.Result)?.data?.size ?: 0

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item =
                (viewModel.searchedForums as? SearchResult.Result)?.data?.get(position) ?: return
            holder.binding.barName.text = item.name
            item.avatarUrl?.let { holder.binding.barAvatar.displayImage(it) }
            holder.binding.barDesc.apply {
                if (!item.desc.isNullOrEmpty()) {
                    text = item.desc
                    isGone = false
                } else {
                    isGone = true
                }
            }
            holder.binding.root.setOnClickListener {
                requireParentFragment().findNavController()
                    .navigate(MobileNavigationDirections.goToForum(item.name))
            }
        }

    }
}