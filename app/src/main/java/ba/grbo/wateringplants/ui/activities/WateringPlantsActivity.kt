package ba.grbo.wateringplants.ui.activities

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import ba.grbo.wateringplants.R
import ba.grbo.wateringplants.databinding.ActivityWateringPlantsBinding
import ba.grbo.wateringplants.ui.viewmodels.WateringPlantsViewModel
import ba.grbo.wateringplants.util.Event
import ba.grbo.wateringplants.util.observeEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WateringPlantsActivity : AppCompatActivity() {
    //region Properties
    private val viewModel: WateringPlantsViewModel by viewModels()
    var onTouchListener: ((event: MotionEvent) -> Unit)? = null
    private lateinit var navController: NavController
    private lateinit var binding: ActivityWateringPlantsBinding
    //endregion

    //region Overriden methods
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_WateringPlants)
        viewModel.observeEvents()
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView<ActivityWateringPlantsBinding>(
            this,
            R.layout.activity_watering_plants
        ).apply {
            lifecycleOwner = this@WateringPlantsActivity
            processItemId = viewModel::processItemId
            actionBarTitleId = viewModel.actionBarTitleId
            setSupportActionBar(materialToolbar)
        }

        val fragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        navController = (fragment as NavHostFragment).navController.apply {
            addOnDestinationChangedListener { _, destination, _ ->
                viewModel.processDestinationId(destination.id)
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        onTouchListener?.invoke(ev)
        return super.dispatchTouchEvent(ev)
    }
    //endregion

    //region LiveData observers
    private fun WateringPlantsViewModel.observeEvents() {
        observeToAnotherFragmentEvent(toAnotherFragmentEvent)
    }

    private fun observeToAnotherFragmentEvent(toFragmentEvent: LiveData<Event<Int>>) {
        observeEvent(toFragmentEvent, this, ::navigateToDestination)
    }
    //endregion

    //region Helper methods
    private fun navigateToDestination(@IdRes destinationId: Int) {
        navController.popBackStack()
        navController.navigate(destinationId)
    }

    fun setBottomNavigationVisibility(visibility: Int) {
        binding.bottomNavigation.visibility = visibility
    }
    //endregion
}