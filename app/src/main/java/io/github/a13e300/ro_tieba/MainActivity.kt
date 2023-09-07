package io.github.a13e300.ro_tieba

import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import io.github.a13e300.ro_tieba.databinding.ActivityMainBinding

const val EXTRA_DONT_USE_NAV = "dont_use_nav"

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /* prevent from navigation clearing our task (maybe dirty) */
        intent.flags = intent.flags.and(Intent.FLAG_ACTIVITY_NEW_TASK.inv())
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val binding: ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun startActivity(intent: Intent?, options: Bundle?) {
        if (options?.getBoolean(EXTRA_DONT_USE_NAV) != true)
            intent?.data?.let { uri ->
                try {
                    findNavController(R.id.nav_host_fragment_activity_main).navigate(
                        uri,
                        NavOptions.Builder()
                            .setEnterAnim(R.anim.nav_default_enter_anim)
                            .setExitAnim(R.anim.nav_default_exit_anim)
                            .setPopEnterAnim(R.anim.nav_default_pop_enter_anim)
                            .setPopExitAnim(R.anim.nav_default_pop_exit_anim)
                            .build()
                    )
                    return
                } catch (ignore: IllegalArgumentException) {
                }
            }
        super.startActivity(intent, options)
    }
}