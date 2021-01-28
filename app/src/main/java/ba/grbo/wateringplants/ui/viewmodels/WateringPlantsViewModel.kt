package ba.grbo.wateringplants.ui.viewmodels

import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ba.grbo.wateringplants.R
import ba.grbo.wateringplants.util.Event

class WateringPlantsViewModel : ViewModel() {
    //region Properties
    private val _toAnotherFragmentEvent = MutableLiveData<Event<@IdRes Int>>()
    val toAnotherFragmentEvent: LiveData<Event<Int>>
        get() = _toAnotherFragmentEvent

    private val _actionBarTitleId = MutableLiveData<@IdRes Int>()
    val actionBarTitleId: LiveData<Int>
        get() = _actionBarTitleId
    //endregion

    //region Helper methods
    fun processItemId(@IdRes itemId: Int) {
        _toAnotherFragmentEvent.value = when (itemId) {
            R.id.plants -> Event(R.id.plantsFragment)
            R.id.encyclopedia -> Event(R.id.encyclopediaFragment)
            R.id.favorites -> Event(R.id.favoritesFragment)
            R.id.settings -> Event(R.id.settingsFragment)
            else -> throw IllegalArgumentException("Unknown itemId: $itemId")
        }
    }

    fun processDestinationId(@IdRes destinationId: Int) {
        when (destinationId) {
            R.id.plantsFragment -> _actionBarTitleId.value = R.string.plants
            R.id.encyclopediaFragment -> _actionBarTitleId.value = R.string.encyclopedia
            R.id.favoritesFragment -> _actionBarTitleId.value = R.string.favorites
            R.id.settingsFragment -> _actionBarTitleId.value = R.string.settings
        }
    }
    //endregion
}