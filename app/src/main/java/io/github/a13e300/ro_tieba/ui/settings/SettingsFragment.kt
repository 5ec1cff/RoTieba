package io.github.a13e300.ro_tieba.ui.settings

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.BuildConfig
import io.github.a13e300.ro_tieba.Logger
import io.github.a13e300.ro_tieba.MobileNavigationDirections
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.databinding.FragmentSettingsBinding
import io.github.a13e300.ro_tieba.utils.copyText
import kotlinx.coroutines.launch

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val child = super.onCreateView(inflater, container, savedInstanceState)
        binding.container.addView(child)
        binding.toolbar.title = getString(R.string.title_settings)
        return binding.root
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)
        findPreference<Preference>("user")?.setOnPreferenceClickListener {
            requireActivity().findNavController(R.id.nav_host_fragment_activity_main)
                .navigate(MobileNavigationDirections.manageAccounts())
            true
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            findPreference<Preference>("configure_default_open")!!.let {
                it.isVisible = true
                it.setOnPreferenceClickListener {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.configure_default_open_title))
                        .setMessage(
                            HtmlCompat.fromHtml(
                                getString(
                                    R.string.configure_default_open_detail,
                                    BuildConfig.APPLICATION_ID
                                ),
                                HtmlCompat.FROM_HTML_MODE_COMPACT
                            )
                        )
                        .setPositiveButton("打开设置") { _, _ ->
                            try {
                                startActivity(Intent("android.settings.MANAGE_DOMAIN_URLS"))
                            } catch (t: Throwable) {
                                Toast.makeText(requireContext(), "无法打开", Toast.LENGTH_SHORT)
                                    .show()
                                Logger.e("manage domain urls", t)
                            }
                        }
                        .setNegativeButton("取消", null)
                        .setNeutralButton("复制命令") { _, _ ->
                            copyText("pm set-app-links-user-selection --package ${BuildConfig.APPLICATION_ID} --user 0 true tieba.baidu.com")
                        }
                        .show()
                    true
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val autoLink = findPreference<SwitchPreference>("auto_link")!!
        autoLink.setOnPreferenceChangeListener { _, newValue ->
            lifecycleScope.launch {
                App.instance.settingsDataStore.updateData {
                    it.toBuilder().setDisableAutoLink(newValue == false).build()
                }
            }
            true
        }
        val autoBv = findPreference<SwitchPreference>("auto_bv")!!
        autoBv.setOnPreferenceChangeListener { _, newValue ->
            lifecycleScope.launch {
                App.instance.settingsDataStore.updateData {
                    it.toBuilder().setDisableAutoBv(newValue == false).build()
                }
            }
            true
        }
        viewLifecycleOwner.lifecycleScope.launch {
            App.instance.settingsDataStore.data.collect {
                autoLink.isChecked = !it.disableAutoLink
                autoBv.isChecked = !it.disableAutoBv
            }
        }
    }
}