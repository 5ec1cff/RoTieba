package io.github.a13e300.ro_tieba

import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment

data class StatusBarConfig(
    val light: Boolean,
    val show: Boolean
)

abstract class BaseFragment : Fragment() {
    protected lateinit var insetsController: WindowInsetsControllerCompat
    override fun onStart() {
        super.onStart()
        insetsController = (requireActivity() as BaseActivity).insetsController
        val config = onInitStatusBar()
        if (config.show)
            insetsController.show(WindowInsetsCompat.Type.statusBars())
        else
            insetsController.hide(WindowInsetsCompat.Type.statusBars())
        insetsController.isAppearanceLightStatusBars = config.light
    }

    protected open fun onInitStatusBar(): StatusBarConfig {
        val ta =
            requireContext().obtainStyledAttributes(intArrayOf(androidx.appcompat.R.attr.isLightTheme))
        val isLight = ta.getBoolean(0, false)
        ta.recycle()
        return StatusBarConfig(
            light = isLight,
            show = true
        )
    }
}