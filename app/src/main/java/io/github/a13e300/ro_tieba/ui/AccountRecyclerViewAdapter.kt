package io.github.a13e300.ro_tieba.ui

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.account.AccountManager
import io.github.a13e300.ro_tieba.databinding.FragmentAccountItemBinding
import io.github.a13e300.ro_tieba.db.Account

@SuppressLint("NotifyDataSetChanged")
class AccountRecyclerViewAdapter(
    private var accounts: List<Account> = listOf(Account()),
    private var mChecked: String = AccountManager.ACCOUNT_ANONYMOUS
) : RecyclerView.Adapter<AccountRecyclerViewAdapter.ViewHolder>() {
    interface OnItemClickedListener {
        fun onItemClicked(item: Account)
        fun onItemLongClicked(view: View, item: Account)
    }

    var listener: OnItemClickedListener? = null

    fun updateAccount(list: List<Account>) {
        accounts = mutableListOf(Account()).apply { addAll(list) }
        notifyDataSetChanged()
    }

    fun updateChecked(uid: String) {
        mChecked = uid
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            FragmentAccountItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ).apply {
                root.setOnClickListener { v ->
                    Logger.d("clicked")
                    (v.getTag(R.id.tag_recycler_view_item) as? Account)?.also {
                        if (it.uid != mChecked)
                            listener?.onItemClicked(it)
                    }
                }
                root.setOnLongClickListener { v ->
                    (v.getTag(R.id.tag_recycler_view_item) as? Account)?.also {
                        if (it.uid != mChecked) {
                            listener?.onItemLongClicked(v, it)
                        }
                    }
                    true
                }
            }
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = accounts[position]
        if (item.uid == AccountManager.ACCOUNT_ANONYMOUS) {
            holder.binding.accountName.text = App.instance.getString(R.string.anonymous)
            holder.binding.accountUid.text = App.instance.getString(R.string.anonymous_tips)
        } else {
            holder.binding.accountName.text = item.name
            holder.binding.accountUid.text = item.uid
        }
        val checked = mChecked == item.uid
        holder.binding.root.isChecked = checked
        holder.itemView.setTag(R.id.tag_recycler_view_item, item)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        listener = null
    }

    override fun getItemCount(): Int = accounts.size

    inner class ViewHolder(val binding: FragmentAccountItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
    }

}