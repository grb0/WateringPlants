package ba.grbo.wateringplants.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ba.grbo.wateringplants.util.Event

class PlantViewModel : ViewModel() {
    //region Properties
    private val _triggerContextualActionBar = MutableLiveData<Boolean>()
    val triggerContextualActionBar: LiveData<Boolean>
        get() = _triggerContextualActionBar

    private val _enterAnimationEndEvent = MutableLiveData<Event<Unit>>()
    val enterAnimationEndEvent: LiveData<Event<Unit>>
        get() = _enterAnimationEndEvent
    //endregion

    //region Helper methods
    fun onEnterAnimationStart() {
        _triggerContextualActionBar.value = true
    }

    fun onEnterAnimationEnd() {
        _enterAnimationEndEvent.value = Event(Unit)
    }
    //endregion
}