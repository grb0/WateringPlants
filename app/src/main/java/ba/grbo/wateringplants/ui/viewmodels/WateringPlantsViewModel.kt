package ba.grbo.wateringplants.ui.viewmodels

import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ba.grbo.wateringplants.R
import ba.grbo.wateringplants.util.Event

class WateringPlantsViewModel : ViewModel() {
    //region Properties
    private val _toPlantsFragmentEvent = MutableLiveData<Event<@IdRes Int>>()
    val toPlantsFragmentEvent: LiveData<Event<Int>>
        get() = _toPlantsFragmentEvent

    private val _toEncyclopediaFragmentEvent = MutableLiveData<Event<@IdRes Int>>()
    val toEncyclopediaFragmentEvent: LiveData<Event<Int>>
        get() = _toEncyclopediaFragmentEvent

    private val _toFavoritesFragmentEvent = MutableLiveData<Event<@IdRes Int>>()
    val toFavoritesFragmentEvent: LiveData<Event<Int>>
        get() = _toFavoritesFragmentEvent

    private val _actionBarTitleId = MutableLiveData<@IdRes Int>()
    val actionBarTitleId: LiveData<Int>
        get() = _actionBarTitleId
    //endregion

    //region Helper methods
    fun processItemId(@IdRes itemId: Int) = when (itemId) {
        R.id.plants -> _toPlantsFragmentEvent.value = Event(R.id.plantsFragment)
        R.id.encyclopedia -> _toEncyclopediaFragmentEvent.value = Event(R.id.encyclopediaFragment)
        R.id.favorites -> _toFavoritesFragmentEvent.value = Event(R.id.favoritesFragment)
        else -> throw IllegalArgumentException("Unknown itemId: $itemId")
    }

    fun processDestinationId(@IdRes destinationId: Int) {
        when (destinationId) {
            R.id.plantsFragment -> _actionBarTitleId.value = R.string.plants
            R.id.encyclopediaFragment -> _actionBarTitleId.value = R.string.encyclopedia
            R.id.favoritesFragment -> _actionBarTitleId.value = R.string.favorites
        }
    }
    //endregion
}