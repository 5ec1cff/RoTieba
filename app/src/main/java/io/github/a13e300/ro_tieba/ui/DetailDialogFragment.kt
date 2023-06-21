package io.github.a13e300.ro_tieba.ui

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.icu.text.DateFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.databinding.DialogDetailBinding
import io.github.a13e300.ro_tieba.databinding.DialogDetailItemContentBinding
import io.github.a13e300.ro_tieba.databinding.DialogDetailItemTitleBinding
import io.github.a13e300.ro_tieba.models.Post
import io.github.a13e300.ro_tieba.models.TiebaThread

fun Post.toDetail() = Pair(
    arrayListOf("pid", "tid", "作者名字", "作者昵称", "IP位置", "楼层", "发布时间"),
    arrayListOf(postId.toString(),
        tid.toString(),
        user.name,
        user.nick,
        user.location,
        floor.toString(),
        time.let { DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(it) }
    )
)

fun TiebaThread.toDetail() = Pair(
    arrayListOf("pid", "tid", "作者名字", "作者昵称", "发布时间", "回复时间"),
    arrayListOf(postId.toString(), tid.toString(), author.name, author.nick,
        createTime?.let {
            DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(it)
        } ?: "",
        time.let { DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(it) }
    )
)


class DetailDialogFragment : DialogFragment() {
    companion object {
        private const val KEY_KEYS = "keys"
        private const val KEY_VALUES = "values"

        fun newInstance(keys: ArrayList<String>, values: ArrayList<String>) =
            DetailDialogFragment().apply {
                assert(keys.size == values.size) { "the size of keys does not match values'" }
                arguments = bundleOf(
                    KEY_KEYS to keys,
                    KEY_VALUES to values
                )
            }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.detail_title)
            .setView(onCreateView(layoutInflater, null, savedInstanceState)).create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DialogDetailBinding.inflate(inflater, container, false)
        binding.detailList.apply {
            layoutManager = GridLayoutManager(requireContext(), 5).also {
                it.spanSizeLookup = object : SpanSizeLookup() {
                    override fun getSpanSize(position: Int) =
                        if (position % 2 == 0) 2
                        else 3
                }
            }
            adapter = Adapter(
                requireArguments().getStringArrayList(KEY_KEYS)!!,
                requireArguments().getStringArrayList(
                    KEY_VALUES
                )!!
            )
        }
        return binding.root
    }

    sealed class ViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root)
    class TitleViewHolder(val binding: DialogDetailItemTitleBinding) : ViewHolder(binding)
    class ContentViewHolder(val binding: DialogDetailItemContentBinding) : ViewHolder(binding)

    class Adapter(private val keys: List<String>, private val values: List<String>) :
        RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            if (viewType == 0) return TitleViewHolder(
                DialogDetailItemTitleBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
            else return ContentViewHolder(DialogDetailItemContentBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ).apply {
                root.setOnClickListener {
                    val context = it.context
                    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(
                        ClipData.newPlainText("", (it as TextView).text)
                    )
                    Toast.makeText(context, "copied", Toast.LENGTH_SHORT).show()
                }
            })
        }

        override fun getItemCount(): Int {
            return keys.size * 2
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            when (holder) {
                is TitleViewHolder -> {
                    holder.binding.root.text = keys[position / 2]
                }

                is ContentViewHolder -> {
                    holder.binding.root.text = values[position / 2]
                }
            }
        }

        override fun getItemViewType(position: Int): Int {
            return position % 2
        }

    }
}