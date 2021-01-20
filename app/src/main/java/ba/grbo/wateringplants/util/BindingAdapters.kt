package ba.grbo.wateringplants.util

import androidx.databinding.BindingAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView

@BindingAdapter("setOnItemSelectedListener")
fun BottomNavigationView.bindSetOnItemSelectedListener(processItemId: (Int) -> Unit) {
    setOnNavigationItemSelectedListener {
        processItemId(it.itemId)
        true
    }

    // Set an empty lambda to avoid calling the listener above
    setOnNavigationItemReselectedListener { }
}