package ba.grbo.wateringplants.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.navigation.fragment.findNavController
import ba.grbo.wateringplants.R
import ba.grbo.wateringplants.databinding.FragmentPlantsBinding
import ba.grbo.wateringplants.ui.activities.WateringPlantsActivity
import ba.grbo.wateringplants.ui.viewmodels.PlantsViewModel
import ba.grbo.wateringplants.util.Event
import ba.grbo.wateringplants.util.observeEvent
import ba.grbo.wateringplants.util.onCreateAnimation

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
        viewModel.observeEvents()
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
    private fun PlantsViewModel.observeEvents() {
        observeEvent(addPlantEvent) { activity.onAddPlant() }
        observeEvent(enterAnimationStartEvent) { this@PlantsFragment.onEnterAnimationStart() }
        observeEvent(enterAnimationEndEvent) { activity.onEnterAnimationEnd() }
    }

    private fun <T> observeEvent(event: LiveData<Event<T>>, block: (T) -> Unit) {
        observeEvent(event, viewLifecycleOwner, block)
    }
    //endregion

    //region Helper methods
    private fun navigateToPlantFragment() {
        findNavController().navigate(PlantsFragmentDirections.actionPlantsFragmentToPlantFragment())
    }

    private fun onEnterAnimationStart() {
        disableViews()
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