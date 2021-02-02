package ba.grbo.wateringplants.util

import android.view.View
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.google.android.material.bottomnavigation.BottomNavigationView

@BindingAdapter("setOnItemSelectedListener")
fun BottomNavigationView.bindSetOnItemSelectedListener(processBottomNavigationItemId: (Int) -> Unit) {
    setOnNavigationItemSelectedListener {
        processBottomNavigationItemId(it.itemId)
        true
    }

    // Set an empty lambda to avoid calling the listener above
    setOnNavigationItemReselectedListener { }
}

@BindingAdapter("visibility")
fun View.bindVisibility(visibility: Boolean?) {
    visibility?.let {
        if ((it && this.visibility != View.VISIBLE) ||
                (!it && this.visibility != View.GONE)
        ) {
            this.visibility = if (it) View.VISIBLE else View.GONE
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