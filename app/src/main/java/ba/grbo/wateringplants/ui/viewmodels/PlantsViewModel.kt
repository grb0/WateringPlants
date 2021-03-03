package ba.grbo.wateringplants.ui.viewmodels

import androidx.annotation.IdRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ba.grbo.wateringplants.R
import ba.grbo.wateringplants.data.Plant
import ba.grbo.wateringplants.data.Result
import ba.grbo.wateringplants.data.source.PlantsRepository
import ba.grbo.wateringplants.util.PlantState
import ba.grbo.wateringplants.util.SharedStateLikeFlow
import ba.grbo.wateringplants.util.SingleSharedFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class PlantsViewModel @Inject constructor(
    repository: PlantsRepository
) : ViewModel() {
    //region Properties
    private val _plantsDB = repository.getAllPlants()
    private val _plants = SharedStateLikeFlow<List<Plant>>()
    val plants: SharedFlow<List<Plant>>
        get() = _plants

    var lastState: PlantState? = null

    init {
        _plantsDB
            .onEach {
                _plants.emit(
                    if (it is Result.Success) it.data
                    else {
                        // TODO notify user of error it as error -> it.exception
                        listOf()
                    }
                )
            }
            .flowOn(Dispatchers.Default)
            .catch { _plants.emit(listOf()) }
            .launchIn(viewModelScope)
    }

    private val _addPlantEvent = SingleSharedFlow<PlantState>()
    val addPlantEvent: SharedFlow<PlantState>
        get() = _addPlantEvent

    private val _viewPlantEvent = SingleSharedFlow<Pair<PlantState, Int>>()
    val viewPlantEvent: SharedFlow<Pair<PlantState, Int>>
        get() = _viewPlantEvent

    private val _removeGlideCacheEvent = SingleSharedFlow<Unit>()
    val removeGlideCacheEvent: SharedFlow<Unit>
        get() = _removeGlideCacheEvent

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
            R.id.add -> {
                _addPlantEvent.tryEmit(PlantState.ADDING)
                lastState = PlantState.ADDING
            }
            R.id.delete_all -> _removeGlideCacheEvent.tryEmit(Unit)
        }
        return true
    }

    fun onEnterAnimationStart() {
        _enterAnimationStartEvent.tryEmit(Unit)
    }

    fun onEnterAnimationEnd() {
        _enterAnimationEndEvent.tryEmit(Unit)
    }

    fun onPlantCardClicked(plantId: Int) {
        _viewPlantEvent.tryEmit(PlantState.VIEWING to plantId)
        lastState = PlantState.VIEWING
    }
    //endregion
}