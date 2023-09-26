package io.github.a13e300.ro_tieba.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.panpf.sketch.displayImage
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.databinding.FragmentSearchUserBinding
import io.github.a13e300.ro_tieba.databinding.FragmentSearchUserItemBinding
import io.github.a13e300.ro_tieba.models.User

class SearchUserFragment : Fragment() {
    private val viewModel: SearchViewModel by viewModels({ requireParentFragment() })
    private val myAdapter = Adapter()
    private lateinit var binding: FragmentSearchUserBinding

    private val searchedUserObserver = Observer<SearchState<List<User>>> {

    }

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
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.searchedUsers.observe(viewLifecycleOwner) {
            when (it) {
                SearchState.Uninitialized -> {
                    binding.resultTips.visibility = View.GONE
                }

                SearchState.Fetching -> {
                    binding.resultTips.visibility = View.VISIBLE
                    binding.resultTips.text = "搜索中"
                    myAdapter.notifyDataSetChanged()
                }

                is SearchState.Result -> {
                    if (it.data.isEmpty()) {
                        binding.resultTips.visibility = View.VISIBLE
                        binding.resultTips.setText(R.string.no_result_tips)
                    } else {
                        binding.resultTips.visibility = View.GONE
                    }
                    myAdapter.notifyDataSetChanged()
                }

                is SearchState.Error -> {
                    binding.resultTips.visibility = View.VISIBLE
                    binding.resultTips.text = it.error.message
                    myAdapter.notifyDataSetChanged()
                }
            }
        }
        viewModel.searchUserEvent.observe(viewLifecycleOwner) { event ->
            event.handle {
                viewModel.fetchUsers(it)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.searchedUsers.removeObservers(viewLifecycleOwner)
        viewModel.searchUserEvent.removeObservers(viewLifecycleOwner)
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
            (viewModel.searchedUsers.value as? SearchState.Result)?.data?.size ?: 0

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item =
                (viewModel.searchedUsers.value as? SearchState.Result)?.data?.get(position)
                    ?: return
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