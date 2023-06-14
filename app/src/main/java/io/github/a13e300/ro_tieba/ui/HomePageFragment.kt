package io.github.a13e300.ro_tieba.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.a13e300.ro_tieba.BaseFragment
import io.github.a13e300.ro_tieba.R
import io.github.a13e300.ro_tieba.databinding.FragmentHomePageBinding

class HomePageFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentHomePageBinding.inflate(inflater, container, false)
        val navView: BottomNavigationView = binding.navView

        val navHostFragment =
            childFragmentManager.findFragmentById(R.id.nav_host_fragment_homepage) as NavHostFragment
        val navController = navHostFragment.navController
        navView.setupWithNavController(navController)
        return binding.root
    }
}