package ba.grbo.wateringplants.ui

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import ba.grbo.wateringplants.R
import ba.grbo.wateringplants.databinding.ActivityWateringPlantsBinding

class WateringPlantsActivity : AppCompatActivity() {
    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_WateringPlants)
        super.onCreate(savedInstanceState)

        val binding: ActivityWateringPlantsBinding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_watering_plants
        )
        val fragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        navController = (fragment as NavHostFragment).navController

        binding.bottomNavigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.plants -> navigate(R.id.plantsFragment)
                R.id.encyclopedia -> navigate(R.id.encyclopediaFragment)
                R.id.favorites -> navigate(R.id.favoritesFragment)
                else -> false
            }
        }
    }

    private fun navigate(@IdRes destination: Int): Boolean {
        navController.popBackStack()
        navController.navigate(destination)
        return true
    }
}