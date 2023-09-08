package io.github.a13e300.ro_tieba.ui.profile

import android.os.Bundle
import android.text.SpannableStringBuilder
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
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.appendSimpleContent
import io.github.a13e300.ro_tieba.databinding.FragmentProfileThreadItemBinding
import io.github.a13e300.ro_tieba.databinding.FragmentProfileThreadsBinding
import io.github.a13e300.ro_tieba.models.TiebaThread
import io.github.a13e300.ro_tieba.toSimpleString
import io.github.a13e300.ro_tieba.ui.forum.ThreadComparator
import kotlinx.coroutines.launch

class ProfileThreadsFragment : Fragment() {
    private val viewModel: ProfileViewModel by viewModels({ requireParentFragment() })
    private lateinit var binding: FragmentProfileThreadsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileThreadsBinding.inflate(inflater, container, false)
        val threadAdapter = ThreadAdapter(ThreadComparator)
        binding.threadList.adapter = threadAdapter
        binding.threadList.layoutManager = LinearLayoutManager(requireContext())
        lifecycleScope.launch {
            viewModel.threadsFlow.collect {
                threadAdapter.submitData(it)
            }
        }
        return binding.root
    }

    inner class ThreadAdapter(
        diffCallback: DiffUtil.ItemCallback<TiebaThread>
    ) : PagingDataAdapter<TiebaThread, ThreadAdapter.ThreadViewHolder>(diffCallback) {
        inner class ThreadViewHolder(val binding: FragmentProfileThreadItemBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onBindViewHolder(holder: ThreadViewHolder, position: Int) {
            val t = getItem(position) ?: return
            holder.binding.threadTitle.text = t.title
            holder.binding.threadInfo.text = "${t.time.toSimpleString()} ${t.forum?.name}吧"
            holder.binding.threadContent.text =
                SpannableStringBuilder().appendSimpleContent(t.content, requireContext())
            holder.binding.root.setOnClickListener {
                findNavController().navigate(MobileNavigationDirections.goToThread(t.tid))
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreadViewHolder {
            return ThreadViewHolder(
                FragmentProfileThreadItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }
}