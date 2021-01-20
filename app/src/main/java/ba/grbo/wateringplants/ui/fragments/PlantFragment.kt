package ba.grbo.wateringplants.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import ba.grbo.wateringplants.R
import ba.grbo.wateringplants.ui.activities.WateringPlantsActivity
import ba.grbo.wateringplants.ui.viewmodels.PlantViewModel
import ba.grbo.wateringplants.util.observeEvent
import ba.grbo.wateringplants.util.onCreateAnimation

class PlantFragment : Fragment() {
    //region Properties
    private val plantViewModel: PlantViewModel by viewModels()
    private lateinit var activity: WateringPlantsActivity
    //endregion

    //region Overriden methods
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        plantViewModel.observeEvents()
        return inflater.inflate(R.layout.fragment_plant, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity = requireActivity() as WateringPlantsActivity
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        return onCreateAnimation(
            transit,
            enter,
            nextAnim,
            requireContext(),
            onExitAnimationEnd = plantViewModel::onExitAnimationEnd,
            onEnterAnimationEnd = plantViewModel::onEnterAnimationEnd,
            superOnCreateAnimation = { t, e, a -> super.onCreateAnimation(t, e, a) }
        )
    }
    //endregion

    //region LiveData observers
    private fun PlantViewModel.observeEvents() {
        observeEvent(exitAnimationEndEvent, requireActivity()) {
            activity.onExitAnimationEnd()
        }
        observeEvent(enterAnimationEndEvent, viewLifecycleOwner) {
            this@PlantFragment.onEnterAnimationEnd()
        }
    }
    //endregion

    //region Helper methods
    private fun WateringPlantsActivity.onExitAnimationEnd() {
        enableBottomNavigation()
    }

    private fun onEnterAnimationEnd() {
        // TODO enable views
    }

    private fun WateringPlantsActivity.enableBottomNavigation() {
        binding.bottomNavigation.visibility = View.VISIBLE
    }
    //endregion
}