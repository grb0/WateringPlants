package ba.grbo.wateringplants.util

import android.content.Context
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

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