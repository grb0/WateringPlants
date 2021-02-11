package ba.grbo.wateringplants.ui.fragments

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
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
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import ba.grbo.wateringplants.R
import ba.grbo.wateringplants.data.Image
import ba.grbo.wateringplants.databinding.FragmentPlantBinding
import ba.grbo.wateringplants.ui.activities.WateringPlantsActivity
import ba.grbo.wateringplants.ui.viewmodels.PlantViewModel
import ba.grbo.wateringplants.util.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class PlantFragment : Fragment() {
    //region Properties
    @Suppress("PrivatePropertyName")
    private val TAKEN_PHOTO_PATH = "TAKEN_PHOTO_PATH"
    private val imageMimeType = "image/*"
    private val plantViewModel: PlantViewModel by viewModels()
    private var actionMode: ActionMode? = null

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
        it?.let { plantViewModel.onImagePathProvided(it.getRealPathFromUriAPI19(requireContext())) }
    }

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) {
        plantViewModel.onTakeImageResultArrival(
            it,
            takenPhotoPath
        )
    }

    private var takenPhotoPath = ""

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
        savedInstanceState?.run { takenPhotoPath = getString(TAKEN_PHOTO_PATH)!! }
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
                plantViewModel.setWateringPeriod(it.toString())
            }

            plantNameEditText.addTextChangedListener {
                plantViewModel.setPlantName(it.toString())
            }

            plantDescriptionEditText.addTextChangedListener {
                plantViewModel.setPlantDescription(it.toString())
            }
        }

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(TAKEN_PHOTO_PATH, takenPhotoPath)
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

    //region Flow collectors
    private fun PlantViewModel.collectFlows() {
        collect(triggerContextualActionBar) {
            triggerContextualActionBar(it.first, it.second.toVisibility)
        }
        collect(plantImage) { if (it.path.isNotEmpty()) loadAndSetPickedImage(it) }
        collect(showImageLoadingProgressEvent) { setImageLoadingProgressVisibility(it.toVisibility) }
        collect(removeCurrentImageEvent) { it?.run { removeCurrentImage() } }
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
        collect(plantName) { setEditText(binding.plantNameEditText, it) }
        collect(plantDescription) { setEditText(binding.plantDescriptionEditText, it) }
        collect(plantWateringPeriod) {
            setEditText(binding.wateringPeriodEditText, it)
            setCalendarText(it)
        }
        collect(showSnackbar) { showSnackbar(it) }
        collect(backToPlantsFragment) { actionMode?.finish() }
    }
    //endregion

    //region Helper methods
    private fun showSnackbar(@StringRes text: Int) {
        showSnackbar(activity.getSnackbarCoordinatorLayout(), text)
    }

    private fun setEditText(view: TextInputEditText, text: String) {
        if (view.text.toString() != text) {
            view.setText(text)
            view.setSelection(text.length)
        }
    }

    private fun setCalendarText(text: String) {
        binding.calendarText.text = text
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

    private fun setImageLoadingProgressVisibility(visibility: Int) {
        binding.imgLoadingProgress.visibility = visibility
    }

    private fun triggerContextualActionBar(@StringRes actionBarTitle: Int, visibility: Int) {
        activity.triggerContextualActionBar(actionBarTitle, visibility)
    }

    private fun loadAndSetPickedImage(image: Image) {
        plantViewModel.showImageLoadingProgressEvent()
        Glide.with(requireContext())
            .load(image.path)
            .transform(RotationTransformation(image.rotationAngle))
            .listener(provideRequestListener())
            .into(binding.plantImg)
    }

    private fun provideRequestListener() = object : RequestListener<Drawable?> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<Drawable?>?,
            isFirstResource: Boolean
        ): Boolean {
            plantViewModel.onImageFailedToLoad()
            return false
        }

        override fun onResourceReady(
            resource: Drawable?,
            model: Any?,
            target: Target<Drawable?>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            plantViewModel.onImageLoadedSuccessfully()
            return false
        }
    }

    private fun removeCurrentImage() {
        binding.plantImg.setImageDrawable(null)
    }

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
        } catch (e: NullPointerException) {
            // TODO notify that uri is null
        }
    }

    private fun getPhotoUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val title = "JPG_${timeStamp}_.jpg"
        val mimeType = "image/jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, title)
            put(MediaStore.Images.Media.DISPLAY_NAME, title)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        }

        val takenPhotoUri = activity.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )!!

        takenPhotoPath = takenPhotoUri.getRealPathFromUriAPI19(requireContext())!!

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
            actionMode = mode
            plantViewModel.onCreatePlantClicked()
        }
        else -> false
    }

    private fun onEnterAnimationEnd() {
        // TODO enable views
    }
    //endregion
}