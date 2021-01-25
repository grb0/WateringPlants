package ba.grbo.wateringplants.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.view.ActionMode
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import ba.grbo.wateringplants.R
import ba.grbo.wateringplants.databinding.FragmentPlantBinding
import ba.grbo.wateringplants.ui.activities.WateringPlantsActivity
import ba.grbo.wateringplants.ui.viewmodels.PlantViewModel
import ba.grbo.wateringplants.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlantFragment : Fragment() {
    //region Properties
    private val plantViewModel: PlantViewModel by viewModels()
    private lateinit var activity: WateringPlantsActivity
    private lateinit var inputMethodManager: InputMethodManager
    private lateinit var binding: FragmentPlantBinding
    //endregion

    //region Overriden methods
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        plantViewModel.observeEvents()
        binding = FragmentPlantBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
//            setOnTouchListener = plantViewModel::setOnTouchListener
            wateringPeriod = plantViewModel.wateringPeriod

            onTouchListener = OnTouchListener(
                ::showKeyboard,
                ::hideKeyboard,
                ::setOnTouchListener,
                ::onReleaseFocus
            )

            onClickWateringPeriodImg = plantViewModel::onClickWateringPeriodImg
            onReleaseFocusWateringPeriodTextInputLayout =
                plantViewModel::onReleaseFocusWateringPeriodTextInputLayout

            plantFragmentConstraintLayout.setOnFocusChangeListener { _, hasFocus ->
                plantViewModel.plantFragmentConstraintLayoutOnFocusChange(hasFocus)
            }
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity = requireActivity() as WateringPlantsActivity
        inputMethodManager =
            activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
    private fun hideKeyboard(view: View) {
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showKeyboard(view: View) {
        lifecycleScope.launch {
            delay(100)
            if (isActive) inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_FORCED)
        }
    }

    private fun setOnTouchListener(onTouchListener: ((MotionEvent) -> Unit)?) {
        activity.onTouchListener = onTouchListener
    }

    private fun releaseOnTouchListener() {
        activity.onTouchListener = null
    }

//    private fun onReleaseFocus(
//        view: View,
//        event: MotionEvent,
//        action: () -> Unit
//    ) {
//        val touchPoint = Point(event.rawX.roundToInt(), event.rawY.roundToInt())
//        val viewTouched = isPointInsideViewBounds(view, touchPoint)
//        if (!viewTouched) action()
//    }
//
//    private fun isPointInsideViewBounds(view: View, point: Point): Boolean = Rect().run {
//        // get view rectangle
//        view.getDrawingRect(this)
//
//        // apply offset
//        IntArray(2).also { locationOnScreen ->
//            view.getLocationOnScreen(locationOnScreen)
//            offset(locationOnScreen[0], locationOnScreen[1])
//        }
//
//        // check is rectangle contains point
//        contains(point.x, point.y)
//    }

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