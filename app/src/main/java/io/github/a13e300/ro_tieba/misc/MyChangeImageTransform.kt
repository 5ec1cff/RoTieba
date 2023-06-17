import android.animation.Animator
import android.view.ViewGroup
import androidx.transition.ChangeImageTransform
import androidx.transition.TransitionValues
import io.github.a13e300.ro_tieba.Logger


private const val PROPNAME_MATRIX = "android:changeImageTransform:matrix"
private const val PROPNAME_BOUNDS = "android:changeImageTransform:bounds"

class MyChangeImageTransform : ChangeImageTransform() {
    override fun captureStartValues(transitionValues: TransitionValues) {
        super.captureStartValues(transitionValues)
        Logger.d("start:view=${transitionValues.view}")
        val bound = transitionValues.values[PROPNAME_BOUNDS]
        Logger.d("start:bound=${bound}")
        val matrix = transitionValues.values[PROPNAME_MATRIX]
        Logger.d("start:matrix=${matrix}")
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        super.captureEndValues(transitionValues)
        Logger.d("end:view=${transitionValues.view}")
        val bound = transitionValues.values[PROPNAME_BOUNDS]
        Logger.d("end:bound=${bound}")
        val matrix = transitionValues.values[PROPNAME_MATRIX]
        Logger.d("end:matrix=${matrix}")
    }

    override fun createAnimator(
        sceneRoot: ViewGroup,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        return super.createAnimator(sceneRoot, startValues, endValues).also {
            Logger.d("createAnimator:$it")
        }
    }
}