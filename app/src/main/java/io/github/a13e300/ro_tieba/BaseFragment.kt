package io.github.a13e300.ro_tieba

import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

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

    protected fun setupToolbar(toolbar: Toolbar) {
        toolbar.setNavigationIcon(R.drawable.ic_back)
        toolbar.setNavigationContentDescription(R.string.back_title)
        toolbar.setNavigationOnClickListener {
            navigateUp()
        }
    }

    fun navigateUp() {
        // we don't like navigate up to home
        if (!findNavController().popBackStack()) {
            activity?.finish()
        }
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