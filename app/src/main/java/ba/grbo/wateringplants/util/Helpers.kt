package ba.grbo.wateringplants.util

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlin.math.roundToInt

fun getAnimation(
    context: Context,
    animation: Int,
    onAnimationStart: () -> Unit,
    onAnimationEnd: () -> Unit,
): Animation = AnimationUtils.loadAnimation(context, animation).apply {
    setAnimationListener(getAnimationListener(onAnimationStart, onAnimationEnd))
}

private fun getAnimationListener(
    onAnimationStart: () -> Unit,
    onAnimationEnd: () -> Unit
) = object : Animation.AnimationListener {
    override fun onAnimationStart(animation: Animation?) {
        onAnimationStart()
    }

    override fun onAnimationEnd(animation: Animation?) {
        onAnimationEnd()
    }

    override fun onAnimationRepeat(animation: Animation?) {
    }
}

fun onCreateAnimation(
    transit: Int,
    enter: Boolean,
    nextAnim: Int,
    context: Context,
    onEnterAnimationStart: () -> Unit,
    onEnterAnimationEnd: () -> Unit,
    superOnCreateAnimation: (Int, Boolean, Int) -> Animation?
): Animation? {
    return if (nextAnim == 0 || !enter) superOnCreateAnimation(transit, enter, nextAnim)
    else getAnimation(
        context,
        nextAnim,
        onEnterAnimationStart,
        onEnterAnimationEnd
    )
}

fun <T> observeEvent(
    event: LiveData<Event<T>>,
    lifecycleOwner: LifecycleOwner,
    block: (T) -> Unit
) {
    event.observe(lifecycleOwner, EventObserver { block(it) })
}

fun <T> observeLiveData(
    liveData: LiveData<T>,
    lifecycleOwner: LifecycleOwner,
    block: (T) -> Unit
) {
    liveData.observe(lifecycleOwner, Observer(block))
}

data class OnTouchListener(
    val showKeyboard: (View) -> Unit,
    val hideKeyBoard: (View) -> Unit,
    val setOnTouchListener: (((MotionEvent) -> Unit)?) -> Unit,
    val onReleaseFocus: (View, MotionEvent, action: () -> Unit) -> Unit
)

fun onReleaseFocus(
    view: View,
    event: MotionEvent,
    action: () -> Unit
) {
    val touchPoint = Point(event.rawX.roundToInt(), event.rawY.roundToInt())
    val viewTouched = isPointInsideViewBounds(view, touchPoint)
    if (!viewTouched) action()
}

private fun isPointInsideViewBounds(view: View, point: Point): Boolean = Rect().run {
    // get view rectangle
    view.getDrawingRect(this)

    // apply offset
    IntArray(2).also { locationOnScreen ->
        view.getLocationOnScreen(locationOnScreen)
        offset(locationOnScreen[0], locationOnScreen[1])
    }

    // check is rectangle contains point
    contains(point.x, point.y)
}

fun removeExcessiveSpace(text: String) = text
    .trim()
    .fold(StringBuilder()) { result, char ->
        if ((char != ' ' && char != '\n') ||
            (char != '\n' && result[result.length - 1] != ' ')
        ) {
            result.append(char)
        } else if ((char == '\n') && result[result.length - 1] != ' ') result.append(' ')
        result
    }
    .toString()
