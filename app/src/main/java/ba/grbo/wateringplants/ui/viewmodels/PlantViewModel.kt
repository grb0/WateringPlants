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

    // first value represents text, second one visibility
    val wateringPeriod = MutableLiveData("1") to MutableLiveData(true)
    //endregion

    //region Helper methods
    fun onEnterAnimationStart() {
        _triggerContextualActionBar.value = true
    }

    fun onEnterAnimationEnd() {
        _enterAnimationEndEvent.value = Event(Unit)
    }

    fun onCalendarImgClick() {
        wateringPeriod.second.value = false
    }

    fun onEditTextReleaseFocus() {
        wateringPeriod.second.value = true
    }

    fun plantFragmentConstraintLayoutOnFocusChange(hasFocus: Boolean) {
        if (hasFocus && wateringPeriod.second.value == false)
            wateringPeriod.second.value = true
    }
    //endregion
}