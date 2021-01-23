package ba.grbo.wateringplants.ui.viewmodels

import androidx.annotation.IdRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ba.grbo.wateringplants.R
import ba.grbo.wateringplants.util.Event

class PlantsViewModel : ViewModel() {
    //region Properties
    private val _addPlantEvent = MutableLiveData<Event<Unit>>()
    val addPlantEvent: LiveData<Event<Unit>>
        get() = _addPlantEvent

    private val _enterAnimationStartEvent = MutableLiveData<Event<Unit>>()
    val enterAnimationStartEvent: LiveData<Event<Unit>>
        get() = _enterAnimationStartEvent

    private val _enterAnimationEndEvent = MutableLiveData<Event<Unit>>()
    val enterAnimationEndEvent: LiveData<Event<Unit>>
        get() = _enterAnimationEndEvent
    //endregion

    //region Helper methods
    fun processActionBarItemId(@IdRes itemId: Int): Boolean {
        when (itemId) {
            R.id.add -> _addPlantEvent.value = Event(Unit)
        }
        return true
    }

    fun onEnterAnimationStart() {
        _enterAnimationStartEvent.value = Event(Unit)
    }

    fun onEnterAnimationEnd() {
        _enterAnimationEndEvent.value = Event(Unit)
    }
    //endregion
}