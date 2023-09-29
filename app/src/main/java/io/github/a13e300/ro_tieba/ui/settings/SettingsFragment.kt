package io.github.a13e300.ro_tieba.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import io.github.a13e300.ro_tieba.App
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.databinding.FragmentSettingsBinding
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
                .navigate(R.id.navigation_accounts)
            true
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