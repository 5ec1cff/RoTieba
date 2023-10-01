package io.github.a13e300.ro_tieba.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.account.AccountManager
import io.github.a13e300.ro_tieba.databinding.FragmentAccountListBinding
import io.github.a13e300.ro_tieba.db.Account
import io.github.a13e300.ro_tieba.misc.OnPreImeBackPressedListener
import io.github.a13e300.ro_tieba.utils.dp2px
import io.github.a13e300.ro_tieba.view.PreImeBackInterceptorView
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class AccountFragment : Fragment(), AccountRecyclerViewAdapter.OnItemClickedListener {
    private fun login(bduss: String) {
        lifecycleScope.launch {
            try {
                App.instance.accountManager.addAccount(bduss)
                Snackbar.make(requireView(), "login success", Snackbar.LENGTH_SHORT).show()
            } catch (t: Throwable) {
                Logger.e("failed when login", t)
                Snackbar.make(requireView(), "Failed to login: ${t.message}", Snackbar.LENGTH_SHORT)
                    .show()
            }
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
                    val context = requireActivity()
                    val dialogContainer = PreImeBackInterceptorView(context)
                    val margin = 16.dp2px(context)
                    val textView = AppCompatEditText(context).apply {
                        layoutParams = ViewGroup.MarginLayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply { setMargins(margin, margin, margin, margin) }
                        maxLines = 1
                    }
                    dialogContainer.addView(textView)
                    val dialog = MaterialAlertDialogBuilder(context)
                        .setTitle("BDUSS")
                        .setView(dialogContainer)
                        .setPositiveButton("Ok") { _, _ ->
                            textView.text?.also { login(it.toString()) }
                        }.create()
                    textView.requestFocus()
                    dialog.window?.apply {
                        clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
                        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
                    }
                    dialogContainer.onBackPressedListener = OnPreImeBackPressedListener {
                        it.context.getSystemService(InputMethodManager::class.java)
                            .hideSoftInputFromWindow(it.windowToken, 0)
                        dialog.hide()
                        true
                    }
                    dialog.show()
                    return true
                }
                return false
            }

        })
        with(binding.list) {
            layoutManager = LinearLayoutManager(context)
            adapter = AccountRecyclerViewAdapter().apply {
                listener = this@AccountFragment
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

    override fun onItemClicked(item: Account) {
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

    override fun onItemLongClicked(view: View, item: Account) {
        val popup = PopupMenu(requireActivity(), view)
        popup.inflate(R.menu.account_item_menu)
        popup.setOnMenuItemClickListener {
            if (it.itemId == R.id.delete_account) {
                if (item.uid != AccountManager.ACCOUNT_ANONYMOUS)
                    lifecycleScope.launch {
                        App.instance.accountManager.deleteAccount(item)
                        Snackbar.make(requireView(), "deleted ${item.name}", Snackbar.LENGTH_SHORT)
                            .show()
                    }
                return@setOnMenuItemClickListener true
            }
            false
        }
        popup.show()
    }
}