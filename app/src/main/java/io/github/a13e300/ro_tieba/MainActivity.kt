package io.github.a13e300.ro_tieba

import android.content.Intent
import android.os.Bundle
import androidx.navigation.findNavController
import io.github.a13e300.ro_tieba.databinding.ActivityMainBinding

const val EXTRA_DONT_USE_NAV = "dont_use_nav"

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding: ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun startActivity(intent: Intent?) {
        intent?.data?.let { uri ->
            try {
                findNavController(R.id.nav_host_fragment_activity_main).navigate(uri)
                return
            } catch (ignore: IllegalArgumentException) {
            }
        }
        super.startActivity(intent)
    }
}