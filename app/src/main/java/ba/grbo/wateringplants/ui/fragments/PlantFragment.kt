package ba.grbo.wateringplants.ui.fragments

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.InsetDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.TypedValue
import android.view.*
import android.view.animation.Animation
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.view.ActionMode
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import ba.grbo.wateringplants.R
import ba.grbo.wateringplants.databinding.FragmentPlantBinding
import ba.grbo.wateringplants.ui.activities.WateringPlantsActivity
import ba.grbo.wateringplants.ui.viewmodels.PlantViewModel
import ba.grbo.wateringplants.util.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class PlantFragment : Fragment() {
    //region Properties
    private val imageMimeType = "image/*"
    private val _takenPhotoUri = "TAKEN_PHOTO_URI"
    private val plantViewModel: PlantViewModel by viewModels()

    private val requestReadExternalStoragePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { plantViewModel.onAskForReadExternalStoragePermissionResultArrival(it) }

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { plantViewModel.onAskForCameraPermissionResultArrival(it) }

    private val requestBothPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { plantViewModel.onAskForBothPermissionsResultArival(it) }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) {
        it?.let {
            realUriPath = it.getRealPathFromUriAPI19(requireContext())
            plantViewModel.onImageUriArrival(it, realUriPath)
        }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        plantViewModel.onTakeImageResultArrival(it)
    }

    private var realUriPath: String? = null
    private var takenPhotoUri: Uri? = null

    private lateinit var activity: WateringPlantsActivity
    private lateinit var imm: InputMethodManager
    private lateinit var binding: FragmentPlantBinding
    private lateinit var popupMenu: PopupMenu
    //endregion

    //region Overriden methods
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        savedInstanceState?.run { takenPhotoUri = getParcelable(_takenPhotoUri) }
        plantViewModel.collectFlows()
        binding = FragmentPlantBinding.inflate(inflater, container, false).apply {
            plantFragmentConstraintLayout.setOnFocusChangeListener { _, hasFocus ->
                plantViewModel.plantFragmentConstraintLayoutOnFocusChange(hasFocus)
            }

            arrayOf(plantNameEditText, plantDescriptionEditText, wateringPeriodEditText).forEach {
                it.setCustomOnFocusChangeListener(
                    ::showKeyboard,
                    ::hideKeyboard,
                    ::setOnTouchListener,
                    plantViewModel::onEditTextReleaseFocus
                )
            }

            popupMenu = PopupMenu(plantImg.context, popupAnchorTxt, Gravity.CENTER).apply {
                inflate(R.menu.popup_plant_img)
                setOnMenuItemClickListener { plantViewModel.processPopupMenuItemId(it.itemId) }

                @Suppress("RestrictedApi")
                (menu as MenuBuilder).run {
                    setOptionalIconsVisible(true)
                    visibleItems.forEach {
                        val iconMarginPx = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            0f,
                            resources.displayMetrics
                        ).toInt()

                        it.icon?.run {
                            it.icon = InsetDrawable(it.icon, iconMarginPx, 0, iconMarginPx, 0)
                        }
                    }
                }
            }

            @Suppress("ClickableViewAccessibility")
            plantImg.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_MOVE -> {
                        popupAnchorTxt.x = event.x - (popupAnchorTxt.width / 2)
                        popupAnchorTxt.y = event.y - (popupAnchorTxt.height / 2)
                    }
                }
                false // listener did not consume the event, will be passed down to onLongClickL...
            }

            calendarImg.setOnClickListener { plantViewModel.onCalendarImgClick() }
            takePhotoImg.setOnClickListener { plantViewModel.onTakePhotoClick() }
            pickImageImg.setOnClickListener { plantViewModel.onPickImageClick() }
            plantImg.setOnLongClickListener { plantViewModel.onPlantImageLongClick() }

            calendarImg.setOnVisibilityChangedListener {
                plantViewModel.setWateringPeriodVisibility(it.toVisibility)
            }

            wateringPeriodLayout.setOnVisibilityChangedListener { view, visibility ->
                if (visibility == View.VISIBLE) view.requestFocus()
                else view.clearFocus()
            }

            wateringPeriodEditText.addTextChangedListener {
                plantViewModel.setWateringPeriodText(it.toString())
            }
        }

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(_takenPhotoUri, takenPhotoUri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = requireActivity() as WateringPlantsActivity
        imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
    private fun PlantViewModel.collectFlows() {
        collect(triggerContextualActionBar) {
            triggerContextualActionBar(it.first, it.second.toVisibility)
        }
        collect(plantImage) {setBitmapToImageView(it)}
        collect(showImageLoadingProgressEvent) { showImageLoadingProgress(it.toVisibility) }
        collect(removeCurrentImageEvent) { it?.run { removeCurrentImage() } }
        collect(requestPickedImageDependenciesEvent) {
            it?.also { providePickedImageDependencies(it) }
        }
        collect(requestTakenPhotoDependenciesEvent) { provideTakenPhotoDependencies() }
        collect(showPickImageTakePhoto) { setPickImageTakePhotoVisibility(it.toVisibility) }
        collect(checkIfReadExternalStoragePermissionWasAlreadyGiven) {
            wasReadExternalStoragePermissionAlreadyGranted()
        }
        collect(checkIfBothPermissionsWereAlreadyGiven) {
            wereBothPermissionsAlreadyGivenResultArrival()
        }
        collect(askForReadExternalStoragePermissionEvent) { askForReadExternalStoragePermission(it) }
        collect(askForCameraPermissionEvent) { askForCameraPermission(it) }
        collect(askForBothPermissionsEvent) { askForBothPermissions(it) }
        collect(showPopupMenuEvent) { showPopupMenu() }
        collect(pickImageEvent) { pickImage() }
        collect(takePhotoEvent) { takePhoto() }
        collect(enterAnimationEndEvent) { this@PlantFragment.onEnterAnimationEnd() }
        collect(wateringPeriodVisibility) { setViewsVisibilities(it) }
        collect(wateringPeriodText) { setViewsTexts(it) }
    }
    //endregion

    //region Helper methods
    private fun setViewsTexts(text: String) {
        binding.calendarText.text = text
        binding.wateringPeriodEditText.setText(text)
        binding.wateringPeriodEditText.setSelection(text.length)
    }

    private fun setViewsVisibilities(visibility: Boolean) {
        binding.calendarImg.visibility = visibility.toVisibility
        binding.wateringPeriodLayout.visibility = (!visibility).toVisibility
        binding.calendarText.visibility = visibility.toVisibility
    }

    private fun setPickImageTakePhotoVisibility(visibility: Int) {
        binding.pickImageImg.visibility = visibility
        binding.takePhotoImg.visibility = visibility
    }

    @Suppress("RestrictedApi")
    private fun showPopupMenu() {
        popupMenu.show()
    }

    private fun showImageLoadingProgress(visibility: Int) {
        binding.imgLoadingProgress.visibility = visibility
    }

    private fun triggerContextualActionBar(@StringRes actionBarTitle: Int, visibility: Int) {
        activity.triggerContextualActionBar(actionBarTitle, visibility)
    }

    private fun setBitmapToImageView(image: Bitmap) {
        binding.plantImg.setImageBitmap(image)
    }

    private fun removeCurrentImage() {
        binding.plantImg.setImageDrawable(null)
    }

    private fun providePickedImageDependencies(uri: Uri, realUriPath: String? = this.realUriPath) {
        binding.plantImg.doOnLayout {
            plantViewModel.onImageDependenciesProvided(
                it.width,
                it.height,
                arrayOf(openInputStream(uri), openInputStream(uri)),
                realUriPath
            )
        }
    }

    private fun provideTakenPhotoDependencies() {
        takenPhotoUri?.let {
            providePickedImageDependencies(it, it.getRealPathFromUriAPI19(requireContext()))
        }
    }

    private fun openInputStream(uri: Uri) = activity.contentResolver.openInputStream(uri)

    private fun pickImage() {
        try {
            pickImage.launch(imageMimeType)
        } catch (e: ActivityNotFoundException) {
            // TODO notify viewmodel, which will request notification of user
        }
    }

    private fun takePhoto() {
        try {
            takePhoto.launch(getPhotoUri())
        } catch (e: ActivityNotFoundException) {
            // TODO notify viewmodel, which will request notification of user
        }
    }

    private fun getPhotoUri(): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val title = "JPG_${timeStamp}_.jpg"
        val mimeType = "image/jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, title)
            put(MediaStore.Images.Media.DISPLAY_NAME, title)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        }

        takenPhotoUri = activity.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )

        return takenPhotoUri
    }

    private fun askForReadExternalStoragePermission(permission: String) {
        requestReadExternalStoragePermission.launch(permission)
    }

    private fun askForCameraPermission(permission: String) {
        requestCameraPermission.launch(permission)
    }

    private fun askForBothPermissions(permissions: Array<String>) {
        requestBothPermissions.launch(permissions)
    }

    private fun wasReadExternalStoragePermissionAlreadyGranted() {
        val wasGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        plantViewModel.onWasReadExternalStoragePermissionWasAlreadyGivenResultArrival(wasGranted)
    }

    private fun wereBothPermissionsAlreadyGivenResultArrival() {
        val wasReadExternalStoragePermissionGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        val wasCameraPermissionGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        plantViewModel.onWereBothPermissionsAlreadyGivenResultArrival(
            wasReadExternalStoragePermissionGranted to wasCameraPermissionGranted
        )
    }

    private fun hideKeyboard(view: View) {
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showKeyboard(view: View) {
        lifecycleScope.launch {
            delay(100)
            if (isActive) imm.showSoftInput(view, InputMethodManager.SHOW_FORCED)
        }
    }

    private fun setOnTouchListener(onTouchListener: ((MotionEvent) -> Unit)?) {
        activity.onTouchListener = onTouchListener
    }

    private fun WateringPlantsActivity.triggerContextualActionBar(
        @StringRes actionbarTitle: Int,
        visibility: Int
    ) {
        setContextualActionBar(actionbarTitle)
        setBottomNavigationVisibility(visibility)
    }

    private fun WateringPlantsActivity.setContextualActionBar(@StringRes actionBarTitle: Int) {
        startSupportActionMode(getActionModeCallback())?.apply {
            title = getText(actionBarTitle)
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
            return processClickedActionItem(mode, item)
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

    private fun processClickedActionItem(mode: ActionMode?, item: MenuItem?) = when (item?.itemId) {
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