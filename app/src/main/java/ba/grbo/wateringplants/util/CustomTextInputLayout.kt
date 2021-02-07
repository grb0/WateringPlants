package ba.grbo.wateringplants.util

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.textfield.TextInputLayout

class CustomTextInputLayout : TextInputLayout {
    private var onVisibilityChangedListener: ((CustomTextInputLayout, Int) -> Unit)? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)
    constructor(context: Context, attributeSet: AttributeSet?, deffStyleAttr: Int) : super(
        context,
        attributeSet,
        deffStyleAttr
    )

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        if (id == changedView.id) onVisibilityChangedListener?.invoke(this, visibility)
        super.onVisibilityChanged(changedView, visibility)
    }

    fun setOnVisibilityChangedListener(listener: ((CustomTextInputLayout, Int) -> Unit)?) {
        onVisibilityChangedListener = listener
    }
}