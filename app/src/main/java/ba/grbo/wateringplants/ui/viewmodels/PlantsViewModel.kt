package ba.grbo.wateringplants.ui.viewmodels

import androidx.annotation.IdRes
import androidx.lifecycle.ViewModel
import ba.grbo.wateringplants.R
import ba.grbo.wateringplants.util.SingleSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class PlantsViewModel : ViewModel() {
    //region Properties
    private val _addPlantEvent = SingleSharedFlow<Unit>()
    val addPlantEvent: SharedFlow<Unit>
        get() = _addPlantEvent

    private val _enterAnimationStartEvent = SingleSharedFlow<Unit>()
    val enterAnimationStartEvent: SharedFlow<Unit>
        get() = _enterAnimationStartEvent

    private val _enterAnimationEndEvent = SingleSharedFlow<Unit>()
    val enterAnimationEndEvent: SharedFlow<Unit>
        get() = _enterAnimationEndEvent
    //endregion

    //region Helper methods
    fun processActionBarItemId(@IdRes itemId: Int): Boolean {
        when (itemId) {
            R.id.add -> _addPlantEvent.tryEmit(Unit)
        }
        return true
    }

    fun onEnterAnimationStart() {
        _enterAnimationStartEvent.tryEmit(Unit)
    }

    fun onEnterAnimationEnd() {
        _enterAnimationEndEvent.tryEmit(Unit)
    }
    //endregion
}