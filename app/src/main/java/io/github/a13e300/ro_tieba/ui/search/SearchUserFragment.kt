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
import io.github.a13e300.ro_tieba.databinding.FragmentSearchUserBinding
import io.github.a13e300.ro_tieba.databinding.FragmentSearchUserItemBinding

class SearchUserFragment : Fragment() {
    private val viewModel: SearchViewModel by viewModels({ requireParentFragment() })
    private val myAdapter = Adapter()
    private lateinit var binding: FragmentSearchUserBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchUserBinding.inflate(inflater, container, false)
        binding.resultList.apply {
            adapter = myAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        viewModel.userLoadState.observe(viewLifecycleOwner) {
            when (it) {
                LoadState.FETCHED -> {
                    binding.resultList.scrollToPosition(0)
                    myAdapter.notifyDataSetChanged()
                    updateData()
                    viewModel.userLoadState.value = LoadState.LOADED
                }

                else -> {}
            }
        }
        updateData()
        return binding.root
    }

    private fun updateData() {
        if (!viewModel.userSearched) {
            return
        }
        viewModel.searchedUsers.let { users ->
            when (users) {
                is SearchResult.Result -> {
                    if (users.data.isEmpty()) {
                        binding.resultTips.visibility = View.VISIBLE
                        binding.resultTips.setText(R.string.no_result_tips)
                    } else {
                        binding.resultTips.visibility = View.GONE
                    }
                }

                is SearchResult.Error -> {
                    binding.resultTips.visibility = View.VISIBLE
                    binding.resultTips.text = users.error.message
                }
            }
        }
    }

    class ViewHolder(val binding: FragmentSearchUserItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class Adapter : RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
            FragmentSearchUserItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

        override fun getItemCount(): Int =
            (viewModel.searchedUsers as? SearchResult.Result)?.data?.size ?: 0

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item =
                (viewModel.searchedUsers as? SearchResult.Result)?.data?.get(position) ?: return
            holder.binding.userName.text = StringBuilder().apply {
                append(item.nick)
                if (item.name.isNotEmpty() && item.name != item.nick) {
                    append("(")
                    append(item.name)
                    append(")")
                }
            }
            item.avatarUrl.let { holder.binding.userAvatar.displayImage(it) }
            holder.binding.userDesc.apply {
                if (!item.desc.isNullOrEmpty()) {
                    text = item.desc
                    isGone = false
                } else {
                    isGone = true
                }
            }
            holder.binding.root.setOnClickListener {
                requireParentFragment().findNavController()
                    .navigate(MobileNavigationDirections.showProfile(item.uidOrPortrait))
            }
        }

    }
}