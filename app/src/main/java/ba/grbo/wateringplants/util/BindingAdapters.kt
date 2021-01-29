package ba.grbo.wateringplants.util

import android.view.View
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import ba.grbo.wateringplants.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText

@BindingAdapter("setOnItemSelectedListener")
fun BottomNavigationView.bindSetOnItemSelectedListener(processBotomNavigationItemId: (Int) -> Unit) {
    setOnNavigationItemSelectedListener {
        processBotomNavigationItemId(it.itemId)
        true
    }

    // Set an empty lambda to avoid calling the listener above
    setOnNavigationItemReselectedListener { }
}

@BindingAdapter(
    "onTouchListener",
    "onReleaseFocus",
    requireAll = false
)
fun TextInputEditText.bindOnTouchListener(
    onTouchListener: OnTouchListener,
    onReleaseFocus: (() -> Unit)?
) {
    setOnFocusChangeListener { _, hasFocus ->
        if (hasFocus) onTouchListener.run {
            showKeyboard(this@bindOnTouchListener)
            setOnTouchListener {
                onReleaseFocus(
                    this@bindOnTouchListener,
                    it,
                    this@bindOnTouchListener::clearFocus
                )
            }
            if (id != R.id.watering_period_edit_text) {
                onReleaseFocus?.invoke()
            }
        } else onTouchListener.run {
            hideKeyBoard(this@bindOnTouchListener)
            setOnTouchListener(null)
            if (id == R.id.plant_description_edit_text) {
                val text = text.toString()
                if (text.isNotEmpty()) setText(removeExcessiveSpace(text))
            }
        }
    }
}

@BindingAdapter("visibility")
fun View.bindVisibility(visibility: Boolean?) {
    visibility?.run {
        if ((visibility && this@bindVisibility.visibility != View.VISIBLE) ||
            (!visibility && this@bindVisibility.visibility != View.GONE)
        ) {
            this@bindVisibility.visibility = if (visibility) View.VISIBLE else View.GONE
        }
    }
}

@InverseBindingAdapter(attribute = "visibility")
fun View.provideVisibility(): Boolean = visibility == View.VISIBLE

@BindingAdapter("visibilityAttrChanged")
fun View.triggerVisibiltiy(attrChange: InverseBindingListener) {
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