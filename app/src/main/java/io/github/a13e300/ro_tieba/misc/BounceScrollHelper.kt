package io.github.a13e300.ro_tieba.misc

import android.animation.ObjectAnimator
import android.util.Property
import android.view.animation.Interpolator
import kotlin.math.abs
import kotlin.math.pow

class BounceScrollHelper {
    var damping = 0f
    var bounceDelay: Long = 0
    var totalHeight: Int = 0
    var overScrolledY = 0
        private set

    fun bounceScroll(dy: Int) {
        if (totalHeight == 0 || damping == 0f) return
        val ratio: Float =
            (1.0f - (abs(overScrolledY.toFloat()) / totalHeight + 0.2f).pow(2)) / damping
        val delta = dy * ratio
        overScrolledY -= delta.toInt()
    }

    /**
     * @return consumed
     */
    fun bouncePreScroll(dy: Int): Int {
        if (totalHeight == 0 || damping == 0f) return 0
        if (dy < 0 && overScrolledY >= 0 || dy > 0 && overScrolledY <= 0) return 0
        // val ratio: Float = (1.0f - (abs(overScrolledY.toFloat()) / totalHeight + 0.2f).pow(2)) / damping
        val delta = dy // * ratio
        val newY = (overScrolledY - delta.toInt()).let {
            if (overScrolledY < 0 && it > 0 || overScrolledY > 0 && it < 0) {
                0
            } else {
                it
            }
        }
        val consumed = (overScrolledY - newY) // .div(ratio).toInt()
        overScrolledY = newY
        return consumed
    }

    fun <T> createScrollBackAnimator(target: T, property: Property<T, Float>): ObjectAnimator =
        ObjectAnimator.ofFloat(target, property, 0f).apply {
            setDuration(bounceDelay).interpolator = DefaultQuartOutInterpolator
            addUpdateListener { overScrolledY = (it.animatedValue as Float).toInt() }
        }

    companion object DefaultQuartOutInterpolator : Interpolator {
        override fun getInterpolation(input: Float): Float {
            return (1.0f - (1 - input).pow(4))
        }
    }
}
