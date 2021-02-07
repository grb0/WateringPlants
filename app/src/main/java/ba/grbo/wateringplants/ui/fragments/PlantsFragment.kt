package ba.grbo.wateringplants.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ba.grbo.wateringplants.R
import ba.grbo.wateringplants.databinding.FragmentPlantsBinding
import ba.grbo.wateringplants.ui.activities.WateringPlantsActivity
import ba.grbo.wateringplants.ui.viewmodels.PlantsViewModel
import ba.grbo.wateringplants.util.collect
import ba.grbo.wateringplants.util.onCreateAnimation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlantsFragment : Fragment() {
    //region Properties
    private val viewModel: PlantsViewModel by viewModels()
    private lateinit var activity: WateringPlantsActivity
    //endregion

    //region Overriden methods
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        viewModel.collectFlows()
        val binding = FragmentPlantsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.app_bar_plants, menu)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = requireActivity() as WateringPlantsActivity
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return viewModel.processActionBarItemId(item.itemId)
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        return onCreateAnimation(
            transit,
            enter,
            nextAnim,
            requireContext(),
            viewModel::onEnterAnimationStart,
            viewModel::onEnterAnimationEnd,
        ) { t, e, a -> super.onCreateAnimation(t, e, a) }
    }
    //endregion

    //region LiveData observers
    private fun PlantsViewModel.collectFlows() {
        collect(addPlantEvent) { onAddPlant() }
        collect(enterAnimationStartEvent) {this@PlantsFragment.onEnterAnimationStart()}
        collect(enterAnimationEndEvent) {this@PlantsFragment.onEnterAnimationEnd()}
    }
    //endregion

    //region Helper methods
    private fun onAddPlant() {
        activity.onAddPlant()
    }

    private fun navigateToPlantFragment() {
        findNavController().navigate(PlantsFragmentDirections.actionPlantsFragmentToPlantFragment())
    }

    private fun onEnterAnimationStart() {
        disableViews()
    }

    private fun onEnterAnimationEnd() {
        activity.onEnterAnimationEnd()
    }

    private fun WateringPlantsActivity.onAddPlant() {
        setBottomNavigationVisibility(View.GONE)
        navigateToPlantFragment()
    }

    private fun WateringPlantsActivity.onEnterAnimationEnd() {
        setBottomNavigationVisibility(View.VISIBLE)
        enableViews()
    }

    private fun disableViews() {
        setEnabled(false)
    }

    private fun enableViews() {
        setEnabled(true)
    }

    private fun setEnabled(state: Boolean) {
    }
    //endregion
}