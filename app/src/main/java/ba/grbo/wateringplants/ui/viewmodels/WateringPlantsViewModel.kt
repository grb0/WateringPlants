package ba.grbo.wateringplants.ui.viewmodels

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import ba.grbo.wateringplants.R
import ba.grbo.wateringplants.util.SharedStateLikeFlow
import ba.grbo.wateringplants.util.SingleSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class WateringPlantsViewModel : ViewModel() {
    //region Properties
    private val _toFragmentEvent = SingleSharedFlow<@IdRes Int>()
    val toFragmentEvent: SharedFlow<Int>
        get() = _toFragmentEvent

    private val _actionBarTitleId = SharedStateLikeFlow<@StringRes Int>()
    val actionBarTitleId = _actionBarTitleId.distinctUntilChanged()
    //endregion

    //region Helper methods
    fun processBottomNavigationItemId(@IdRes itemId: Int): Boolean {
        _toFragmentEvent.tryEmit(
            when (itemId) {
                R.id.plants -> R.id.plantsFragment
                R.id.encyclopedia -> R.id.encyclopediaFragment
                R.id.favorites -> R.id.favoritesFragment
                R.id.settings -> R.id.settingsFragment
                else -> throw IllegalArgumentException("Unknown itemId: $itemId")
            }
        )
        return true
    }

    fun processDestinationId(@IdRes destinationId: Int) {
        when (destinationId) {
            R.id.plantsFragment -> _actionBarTitleId.tryEmit(R.string.plants)
            R.id.encyclopediaFragment -> _actionBarTitleId.tryEmit(R.string.encyclopedia)
            R.id.favoritesFragment -> _actionBarTitleId.tryEmit(R.string.favorites)
            R.id.settingsFragment -> _actionBarTitleId.tryEmit(R.string.settings)
        }
    }
    //endregion
}