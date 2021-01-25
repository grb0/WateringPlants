package ba.grbo.wateringplants.util

import android.util.Log
import android.view.View
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import ba.grbo.wateringplants.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText

@BindingAdapter("setOnItemSelectedListener")
fun BottomNavigationView.bindSetOnItemSelectedListener(processItemId: (Int) -> Unit) {
    setOnNavigationItemSelectedListener {
        processItemId(it.itemId)
        true
    }

    // Set an empty lambda to avoid calling the listener above
    setOnNavigationItemReselectedListener { }
}

@BindingAdapter(
    "setOnTouchListener",
    "setOnReleaseFocusWateringPeriodTextInputLayout",
    requireAll = false
)
fun TextInputEditText.bindOnSetTouchListener(
    onTouchListener: OnTouchListener,
    onReleaseFocusWateringPeriodTextInputLayout: (() -> Unit)?
) {
    setOnFocusChangeListener { _, hasFocus ->
        if (hasFocus) onTouchListener.run {
            showKeyboard(this@bindOnSetTouchListener)
            setOnTouchListener {
                onReleaseFocus(
                    this@bindOnSetTouchListener,
                    it,
                    this@bindOnSetTouchListener::clearFocus
                )
            }
            if (id != R.id.watering_period_text_input_edit_text) {
                onReleaseFocusWateringPeriodTextInputLayout?.invoke()
            }
        } else onTouchListener.run {
            hideKeyBoard(this@bindOnSetTouchListener)
            setOnTouchListener(null)
            onIdMatch(id, this@bindOnSetTouchListener)
        }
    }
}

private fun onIdMatch(
    id: Int,
    view: TextInputEditText
) {
    if (id == R.id.plant_description_text_input_edit_text && view.text.toString().isNotEmpty())
        view.setText(removeExcessiveSpace(view.text.toString()))
}

@BindingAdapter("visualness")
fun View.bindVisualness(visualness: Boolean?) {
    visualness?.run {
        if ((visualness && visibility != View.VISIBLE) ||
            (!visualness && visibility != View.GONE)
        ) {
            visibility = if (visualness) View.VISIBLE else View.GONE
        }
    }
}

@InverseBindingAdapter(attribute = "visualness")
fun View.getVisualness(): Boolean = visibility == View.VISIBLE

@BindingAdapter("visualnessAttrChanged")
fun View.triggerVisualness(attrChange: InverseBindingListener) {
    when (this) {
        is CustomTextView -> setOnVisibilityChangedListener { attrChange.onChange() }
        is CustomTextInputLayout -> setOnVisibilityChangedListener {
            attrChange.onChange()
            if (it == View.VISIBLE) requestFocus()
            else clearFocus()
        }
        is CustomImageView -> setOnVisibilityChangedListener { attrChange.onChange() }
    }
}