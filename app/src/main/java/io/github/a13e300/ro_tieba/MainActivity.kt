package io.github.a13e300.ro_tieba

import android.os.Bundle
import io.github.a13e300.ro_tieba.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Logger.d("density:${resources.displayMetrics.density}")
        super.onCreate(savedInstanceState)

        val binding: ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}