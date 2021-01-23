package ba.grbo.wateringplants.ui.fragments

import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import ba.grbo.wateringplants.R
import ba.grbo.wateringplants.ui.activities.WateringPlantsActivity
import ba.grbo.wateringplants.ui.viewmodels.PlantViewModel
import ba.grbo.wateringplants.util.observeEvent
import ba.grbo.wateringplants.util.observeLiveData
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
            plantViewModel::onEnterAnimationStart,
            plantViewModel::onEnterAnimationEnd
        ) { t, e, a -> super.onCreateAnimation(t, e, a) }
    }
    //endregion

    //region LiveData observers
    private fun PlantViewModel.observeEvents() {
        observeLiveData(triggerContextualActionBar, viewLifecycleOwner) {
            if (it) activity.triggerContextualActionBar()
        }
        observeEvent(enterAnimationEndEvent, viewLifecycleOwner) {
            this@PlantFragment.onEnterAnimationEnd()
        }
    }
    //endregion

    //region Helper methods
    private fun WateringPlantsActivity.triggerContextualActionBar() {
        setContextualActionBar()
        setBottomNavigationVisibility(View.GONE)
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
            return this@PlantFragment.onActionItemClicked(mode, item)
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            onDestroyActionMode()
        }
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

    private fun onEnterAnimationEnd() {
        // TODO enable views
    }
    //endregion
}