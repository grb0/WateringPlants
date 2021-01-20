package ba.grbo.wateringplants.ui.fragments

import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ba.grbo.wateringplants.R
import ba.grbo.wateringplants.databinding.FragmentPlantsBinding
import ba.grbo.wateringplants.ui.activities.WateringPlantsActivity
import ba.grbo.wateringplants.ui.viewmodels.PlantsViewModel
import ba.grbo.wateringplants.util.observeEvent
import ba.grbo.wateringplants.util.onCreateAnimation

class PlantsFragment : Fragment() {
    //region Properties
    private val plantsViewModel: PlantsViewModel by viewModels()
    private lateinit var activity: WateringPlantsActivity
    //endregion

    //region Overriden methods
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        plantsViewModel.observeEvents()
        val binding = FragmentPlantsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.app_bar_plants, menu)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity = requireActivity() as WateringPlantsActivity
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return plantsViewModel.processActionBarItemId(item.itemId)
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        return onCreateAnimation(
            transit,
            enter,
            nextAnim,
            requireContext(),
            plantsViewModel::onExitAnimationStart,
            onEnterAnimationStart = plantsViewModel::onEnterAnimationStart,
            onEnterAnimationEnd = plantsViewModel::onEnterAnimationEnd,
            superOnCreateAnimation = { t, e, a -> super.onCreateAnimation(t, e, a) }
        )
    }
    //endregion

    //region LiveData observers
    private fun PlantsViewModel.observeEvents() {
        observeEvent(addPlantEvent, viewLifecycleOwner) {
            this@PlantsFragment.navigateToPlantFragment()
        }
        observeEvent(exitAnimationStartEvent, requireActivity()) {
            activity.onExitAnimationStart()
        }
        observeEvent(enterAnimationStartEvent, viewLifecycleOwner) {
            this@PlantsFragment.onEnterAnimationStart()
        }
        observeEvent(enterAnimationEndEvent, viewLifecycleOwner) {
            this@PlantsFragment.onEnterAnimationEnd()
        }
    }
    //endregion

    //region Helper methods
    private fun navigateToPlantFragment() {
        findNavController().navigate(PlantsFragmentDirections.actionPlantsFragmentToPlantFragment())
    }

    private fun WateringPlantsActivity.onDestroyActionMode() {
        onBackPressed()
    }

    private fun WateringPlantsActivity.onCreateActionMode(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_bar_plant, menu)
        return true
    }

    private fun onPrepareActionMode() = false

    private fun onActionItemClicked(mode: ActionMode?, item: MenuItem?) = when (item?.itemId) {
        R.id.create -> {
            mode?.finish()
            true
        }
        else -> false
    }

    private fun WateringPlantsActivity.onExitAnimationStart() {
        disableBottomNavigation()
        setContextualActionBar()
    }

    private fun onEnterAnimationStart() {
        disableViews()
    }

    private fun onEnterAnimationEnd() {
        enableViews()
    }

    private fun WateringPlantsActivity.disableBottomNavigation() {
        binding.bottomNavigation.visibility = View.GONE
    }

    private fun WateringPlantsActivity.setContextualActionBar() {
        startSupportActionMode(getActionModeCallback())?.apply {
            title = getText(R.string.add_plant)
        }
    }

    private fun WateringPlantsActivity.getActionModeCallback() = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return onCreateActionMode(menu)
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return onPrepareActionMode()
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return this@PlantsFragment.onActionItemClicked(mode, item)
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            onDestroyActionMode()
        }
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