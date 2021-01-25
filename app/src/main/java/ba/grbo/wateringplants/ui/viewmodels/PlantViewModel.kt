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

    private val _setOnTouchListener = MutableLiveData<Boolean>()
    val setOnTouchListener: LiveData<Boolean>
        get() = _setOnTouchListener

    val wateringPeriod = WateringPeriod(MutableLiveData("1"), MutableLiveData(true))
    //endregion

    //region Helper methods
    fun onEnterAnimationStart() {
        _triggerContextualActionBar.value = true
    }

    fun onEnterAnimationEnd() {
        _enterAnimationEndEvent.value = Event(Unit)
    }

    fun setOnTouchListener(value: Boolean) {
        _setOnTouchListener.value = value
    }

    fun onClickWateringPeriodImg() {
        wateringPeriod.visibility.value = false
    }

    fun onReleaseFocusWateringPeriodTextInputLayout() {
        wateringPeriod.visibility.value = true
    }

    fun plantFragmentConstraintLayoutOnFocusChange(hasFocus: Boolean) {
        if (hasFocus && wateringPeriod.visibility.value == false)
            wateringPeriod.visibility.value = true
    }
    //endregion
}

data class WateringPeriod(
    val text: MutableLiveData<String>,
    val visibility: MutableLiveData<Boolean>
)