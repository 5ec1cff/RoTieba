package io.github.a13e300.ro_tieba.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.databinding.FragmentSettingsBinding

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
        setPreferencesFromResource(R.xml.preference, rootKey);
        findPreference<Preference>("user")?.setOnPreferenceClickListener {
            requireActivity().findNavController(R.id.nav_host_fragment_activity_main)
                .navigate(R.id.navigation_accounts)
            true
        }
    }
}