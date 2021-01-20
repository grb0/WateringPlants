package ba.grbo.wateringplants.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ba.grbo.wateringplants.util.Event

class PlantViewModel : ViewModel() {
    //region Properties
    private val _exitAnimationEndEvent = MutableLiveData<Event<Unit>>()
    val exitAnimationEndEvent: LiveData<Event<Unit>>
        get() = _exitAnimationEndEvent

    private val _enterAnimationEndEvent = MutableLiveData<Event<Unit>>()
    val enterAnimationEndEvent: LiveData<Event<Unit>>
        get() = _enterAnimationEndEvent
    //endregion

    //region Helper methods
    fun onExitAnimationEnd() {
        _exitAnimationEndEvent.value = Event(Unit)
    }

    fun onEnterAnimationEnd() {
        _enterAnimationEndEvent.value = Event(Unit)
    }
    //endregion
}