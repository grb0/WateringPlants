package ba.grbo.wateringplants.ui.activities

import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import ba.grbo.wateringplants.R
import ba.grbo.wateringplants.databinding.ActivityWateringPlantsBinding
import ba.grbo.wateringplants.ui.viewmodels.WateringPlantsViewModel
import ba.grbo.wateringplants.util.collect
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

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
        viewModel.collectFlows()
        super.onCreate(savedInstanceState)

        binding = ActivityWateringPlantsBinding.inflate(layoutInflater).apply {
            setSupportActionBar(materialToolbar)

            bottomNavigation.setOnNavigationItemSelectedListener {
                viewModel.processBottomNavigationItemId(it.itemId)
            }

            // Set an empty lambda to avoid calling the listener above
            bottomNavigation.setOnNavigationItemReselectedListener { }
        }

        val fragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        navController = (fragment as NavHostFragment).navController.apply {
            addOnDestinationChangedListener { _, destination, _ ->
                viewModel.processDestinationId(destination.id)
            }
        }

        setContentView(binding.root)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        onTouchListener?.invoke(ev)
        return super.dispatchTouchEvent(ev)
    }
    //endregion

    //region LiveData observers
    private fun WateringPlantsViewModel.collectFlows() {
        collect(toFragmentEvent) { navigateToDestination(it) }
        collect(actionBarTitleId) {
            delay(1) // Delay so that our title does not get overriden by default
            setMaterialToolbarTitle(it) }
    }
    //endregion

    //region Helper methods
    private fun navigateToDestination(@IdRes destinationId: Int) {
        navController.popBackStack()
        navController.navigate(destinationId)
    }

    private fun setMaterialToolbarTitle(@StringRes title: Int) {
        binding.materialToolbar.title = getString(title)
    }

    fun setBottomNavigationVisibility(visibility: Int) {
        binding.bottomNavigation.visibility = visibility
    }
    //endregion
}