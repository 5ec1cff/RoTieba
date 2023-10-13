package io.github.a13e300.ro_tieba.ui.account

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.account.AccountManager
import io.github.a13e300.ro_tieba.databinding.DialogAccountInfoBinding
import io.github.a13e300.ro_tieba.databinding.DialogLoginMethodBinding
import io.github.a13e300.ro_tieba.databinding.FragmentAccountItemBinding
import io.github.a13e300.ro_tieba.databinding.FragmentAccountListBinding
import io.github.a13e300.ro_tieba.db.Account
import io.github.a13e300.ro_tieba.misc.OnPreImeBackPressedListener
import io.github.a13e300.ro_tieba.utils.copyText
import io.github.a13e300.ro_tieba.utils.forceShowIcon
import io.github.a13e300.ro_tieba.view.PreImeBackInterceptorView
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class AccountFragment : Fragment() {
    private fun login(bduss: String, sToken: String? = null, baiduId: String? = null) {
        lifecycleScope.launch {
            try {
                App.instance.accountManager.addAccount(bduss, sToken, baiduId)
                Snackbar.make(requireView(), "登录成功", Snackbar.LENGTH_SHORT).show()
            } catch (t: Throwable) {
                Logger.e("failed when login", t)
                Snackbar.make(requireView(), "登录失败: ${t.message}", Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun loginWithWebView() {
        findNavController().navigate(MobileNavigationDirections.login())
        setFragmentResultListener("login") { _, b ->
            if (b.getBoolean("success", false)) {
                Snackbar.make(requireView(), "登录成功", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoginMethodDialog() {
        val dialogBinding = DialogLoginMethodBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择登录方式")
            .setView(dialogBinding.root)
            .show()

        dialogBinding.loginWithCookies.setOnClickListener {
            showAccountInfoDialog(true, null)
            dialog.dismiss()
        }
        dialogBinding.loginWithWebview.setOnClickListener {
            loginWithWebView()
            dialog.dismiss()
        }
    }

    private fun showAccountInfoDialog(login: Boolean, account: Account?) {
        // TODO: import cookies from file
        val dialogBinding = DialogAccountInfoBinding.inflate(layoutInflater)
        val view = if (login) {
            val dialogContainer = PreImeBackInterceptorView(requireContext())
            dialogContainer.addView(dialogBinding.root)
            dialogContainer
        } else dialogBinding.root
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (login) "输入 cookies" else "编辑账户信息")
            .setView(view)
            .setPositiveButton(if (login) "登录" else "确定") { _, _ ->
                this@AccountFragment.login(
                    dialogBinding.bdussText.text.toString(),
                    dialogBinding.stokenText.text?.toString(),
                    dialogBinding.baiduidText.text?.toString()
                )
            }
            .setNegativeButton("取消", null)
            .create()
        dialogBinding.apply {
            nameLayout.isVisible = !login
            uidLayout.isVisible = !login
            account?.let { ac ->
                nameText.setText(ac.name)
                uidText.setText(ac.uid)
                bdussText.setText(ac.bduss)
                stokenText.setText(ac.stoken)
                baiduidText.setText(ac.baiduId)
                bdussLayout.hint = "BDUSS"
                nameLayout.setOnClickListener {
                    copyText(ac.name ?: "")
                }
                nameText.setOnClickListener {
                    copyText(ac.name ?: "")
                }
                uidLayout.setOnClickListener {
                    copyText(ac.uid)
                }
                uidText.setOnClickListener {
                    copyText(ac.uid)
                }
            }
            if (login) {
                bdussText.requestFocus()
                dialog.window?.apply {
                    clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
                    setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
                }
                (view as PreImeBackInterceptorView).onBackPressedListener =
                    OnPreImeBackPressedListener {
                        it.context.getSystemService(InputMethodManager::class.java)
                            .hideSoftInputFromWindow(it.windowToken, 0)
                        dialog.hide()
                        true
                    }
            }
            dialog.show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentAccountListBinding.inflate(inflater, container, false)
        binding.toolbar.title = getString(R.string.account_label)
        binding.toolbar.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.accounts_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.add_account) {
                    showLoginMethodDialog()
                    return true
                }
                return false
            }

        })
        with(binding.list) {
            layoutManager = LinearLayoutManager(context)
            adapter = AccountRecyclerViewAdapter().apply {
                viewLifecycleOwner.lifecycleScope.launch {
                    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        launch {
                            App.instance.db.accountDao().getAccounts().distinctUntilChanged()
                                .collect {
                                    updateAccount(it)
                                }
                        }
                        launch {
                            App.instance.accountManager.currentAccount.collect {
                                updateChecked(it.uid)
                            }
                        }
                    }
                }

            }
        }
        return binding.root
    }

    fun onItemClicked(item: Account) {
        lifecycleScope.launch {
            try {
                App.instance.accountManager.switchAccount(item.uid)
                Snackbar.make(requireView(), "切换到${item.name ?: "匿名"}", Snackbar.LENGTH_SHORT)
                    .show()
            } catch (t: Throwable) {
                Logger.e("failed to switch to account", t)
                Snackbar.make(requireView(), "无法切换账户，请检查网络", Snackbar.LENGTH_SHORT)
                    .show()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    inner class AccountRecyclerViewAdapter(
        private var accounts: List<Account> = listOf(Account()),
        private var mChecked: String = AccountManager.ACCOUNT_ANONYMOUS
    ) : RecyclerView.Adapter<AccountRecyclerViewAdapter.ViewHolder>() {

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
                    registerForContextMenu(root)
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
            holder.binding.root.setOnClickListener {
                onItemClicked(item)
            }
            if (item.uid == AccountManager.ACCOUNT_ANONYMOUS) return
            holder.binding.root.setOnCreateContextMenuListener { contextMenu, _, _ ->
                MenuInflater(requireContext()).inflate(R.menu.account_item_menu, contextMenu)
                contextMenu.forceShowIcon()
                contextMenu.findItem(R.id.delete).apply {
                    isVisible = !checked
                    setOnMenuItemClickListener {
                        lifecycleScope.launch {
                            App.instance.accountManager.deleteAccount(item)
                            Snackbar.make(
                                requireView(),
                                "deleted ${item.name}",
                                Snackbar.LENGTH_SHORT
                            )
                                .show()
                        }
                        return@setOnMenuItemClickListener true
                    }
                }
                contextMenu.findItem(R.id.edit_account).apply {
                    setOnMenuItemClickListener {
                        showAccountInfoDialog(false, item)
                        true
                    }
                }
                contextMenu.findItem(R.id.open_profile).apply {
                    setOnMenuItemClickListener {
                        findNavController().navigate(MobileNavigationDirections.showProfile(item.uid))
                        true
                    }
                }
            }
        }

        override fun getItemCount(): Int = accounts.size

        inner class ViewHolder(val binding: FragmentAccountItemBinding) :
            RecyclerView.ViewHolder(binding.root) {
        }

    }
}