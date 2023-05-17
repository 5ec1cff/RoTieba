package io.github.a13e300.ro_tieba.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
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
                    val textView = AppCompatEditText(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        maxLines = 1
                    }
                    MaterialAlertDialogBuilder(context)
                        .setTitle("BDUSS")
                        .setView(textView)
                        .setPositiveButton("Ok") { _, _ ->
                            textView.text?.also { login(it.toString()) }
                        }
                        .show()
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
                                Logger.d("current account=$it")
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
            App.instance.accountManager.switchAccount(item.uid)
            Snackbar.make(requireView(), "switch to ${item.name}", Snackbar.LENGTH_SHORT).show()
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