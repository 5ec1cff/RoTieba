package io.github.a13e300.ro_tieba

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.LayoutInflaterCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import rikka.insets.WindowInsetsHelper
import rikka.layoutinflater.view.LayoutInflaterFactory

abstract class BaseActivity : AppCompatActivity() {
    lateinit var insetsController: WindowInsetsControllerCompat
    override fun onCreate(savedInstanceState: Bundle?) {
        /*
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.transparent)
        window.navigationBarColor = ContextCompat.getColor(this, android.R.color.transparent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        */
        insetsController = WindowCompat.getInsetsController(window, window.decorView)
        LayoutInflaterCompat.setFactory2(
            layoutInflater, LayoutInflaterFactory(delegate)
                .addOnViewCreatedListener(WindowInsetsHelper.LISTENER)
        )
        super.onCreate(savedInstanceState)
    }
}