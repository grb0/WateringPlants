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
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import ba.grbo.wateringplants.R
import com.google.android.material.textfield.TextInputEditText
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

fun TextInputEditText.setCustomOnFocusChangeListener(
        showKeyboard: (View) -> Unit,
        hideKeyboard: (View) -> Unit,
        setOnTouchListener: (((MotionEvent) -> Unit)?) -> Unit,
        onEditTextReleaseFocus: () -> Unit
) {
    setOnFocusChangeListener { _, hasFocus ->
        if (hasFocus) {
            showKeyboard(this)
            setOnTouchListener { onReleaseFocus(this, it, ::clearFocus) }
            if (id != R.id.watering_period_edit_text) onEditTextReleaseFocus()
        } else {
            hideKeyboard(this)
            setOnTouchListener(null)
            when (id) {
                R.id.plant_name_edit_text,
                R.id.plant_description_edit_text -> {
                    val text = text.toString()
                    if (text.isNotEmpty()) setText(removeExcessiveSpace(text))
                }
            }
        }
    }
}

fun <T, K, R> LiveData<T>.combineWith(
        liveData: LiveData<K>,
        block: (T?, K?) -> R
): LiveData<R> = MediatorLiveData<R>().also { mld ->
    mld.addSource(this) { mld.value = block(value, liveData.value) }
    mld.addSource(liveData) { mld.value = block(value, liveData.value) }
}

val Float.hasToBeRotated: Boolean
    get() = this != 0f

val Boolean.toVisibility: Int
    get() = if (this) View.VISIBLE else View.GONE