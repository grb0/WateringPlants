package ba.grbo.wateringplants.util

import android.content.Context
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData

fun getAnimation(
    context: Context,
    animation: Int,
    onAnimationStart: () -> Unit = {},
    onAnimationEnd: () -> Unit = {},
    onAnimationRepeat: () -> Unit = {}
): Animation = AnimationUtils.loadAnimation(context, animation).apply {
    setAnimationListener(
        getAnimationListener(
            onAnimationStart,
            onAnimationEnd,
            onAnimationRepeat
        )
    )
}

private fun getAnimationListener(
    onAnimationStart: () -> Unit = {},
    onAnimationEnd: () -> Unit = {},
    onAnimationRepeat: () -> Unit = {}
) = object : Animation.AnimationListener {
    override fun onAnimationStart(animation: Animation?) {
        onAnimationStart()
    }

    override fun onAnimationEnd(animation: Animation?) {
        onAnimationEnd()
    }

    override fun onAnimationRepeat(animation: Animation?) {
        onAnimationRepeat()
    }
}

fun onCreateAnimation(
    transit: Int,
    enter: Boolean,
    nextAnim: Int,
    context: Context,
    onExitAnimationStart: () -> Unit = {},
    onExitAnimationEnd: () -> Unit = {},
    onExitAnimationRepeat: () -> Unit = {},
    onEnterAnimationStart: () -> Unit = {},
    onEnterAnimationEnd: () -> Unit = {},
    onEnterAnimationRepeat: () -> Unit = {},
    superOnCreateAnimation: (Int, Boolean, Int) -> Animation?
): Animation? {
    return if (nextAnim == 0 || !enter) {
        if (nextAnim != 0) getAnimation(
            context,
            nextAnim,
            onExitAnimationStart,
            onExitAnimationEnd,
            onExitAnimationRepeat
        ) else superOnCreateAnimation(transit, enter, nextAnim)
    } else getAnimation(
        context,
        nextAnim,
        onEnterAnimationStart,
        onEnterAnimationEnd,
        onEnterAnimationRepeat
    )
}

fun <T> observeEvent(
    event: LiveData<Event<T>>,
    lifecycleOwner: LifecycleOwner,
    block: (T) -> Unit
) {
    event.observe(lifecycleOwner, EventObserver { block(it) })
}