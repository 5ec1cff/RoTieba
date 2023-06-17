package io.github.a13e300.ro_tieba.utils

import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.view.isGone
import io.github.a13e300.ro_tieba.R

fun View.showAnim(down: Boolean) {
    val anim = AnimationUtils.loadAnimation(
        context,
        if (down) R.anim.slide_in_down else R.anim.slide_in_up
    )
    isGone = false
    startAnimation(anim)
}

fun View.hideAnim(down: Boolean) {
    val anim = AnimationUtils.loadAnimation(
        context,
        if (down) R.anim.slide_out_down else R.anim.slide_out_up
    )
    anim.setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationStart(p0: Animation?) {
        }

        override fun onAnimationEnd(p0: Animation?) {
            isGone = true
        }

        override fun onAnimationRepeat(p0: Animation?) {
        }
    })
    startAnimation(anim)
}
