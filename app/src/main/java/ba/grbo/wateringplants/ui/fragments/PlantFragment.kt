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
import androidx.annotation.*
import androidx.appcompat.view.ActionMode
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

    private lateinit var actionModeCloseButton: AppCompatImageView
    private lateinit var actionModeTitle: AppCompatTextView

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

    override fun setArguments(args: Bundle?) {
        if (args != null) super.setArguments(Bundle().apply { putBundle(BUNDLE_ARGS, args) })
        else super.setArguments(null)
    }
    //endregion

    //region Flow collectors
    private fun PlantViewModel.collectFlows() {
        collect(removeBottomNavigation) { setBottomNavigationVisibility(it) }
        collect(triggerAddingContextualActionBar) { triggerAddingContextualActionBar() }
        collect(triggerViewingContextualActionbar) { triggerViewingContextualActionBar(it) }
        collect(adjustToEditingContextualActionBar) {
            if (plantState.value.first == PlantState.EDITING) adjustToEditingContextualActionBar()
        }
        collect(adjustToViewingContextualActionbar) {
            if (plantState.value.first == PlantState.VIEWING) adjustToViewingContextualActionbar(it)
        }
        collect(collectPlantFlowsEvent) { collectPlantFlows() }
        collect(showImageLoadingProgressEvent) {
            setImageLoadingProgressVisibility(it.toVisibility)
        }
        collect(removeCurrentImageEvent) { it?.run { removeCurrentImage() } }
        collect(confirmReverseEvent) { if (it) confirmChangesReversal() }
        collect(showPickImageTakePhoto) { setPickImageTakePhotoVisibility(it.toVisibility) }
        collect(checkIfReadExternalStoragePermissionWasAlreadyGiven) {
            wasReadExternalStoragePermissionAlreadyGranted()
        }
        collect(checkIfBothPermissionsWereAlreadyGiven) {
            wereBothPermissionsAlreadyGivenResultArrival()
        }
        collect(askForReadExternalStoragePermissionEvent) {
            askForReadExternalStoragePermission(it)
        }
        collect(askForCameraPermissionEvent) { askForCameraPermission(it) }
        collect(askForBothPermissionsEvent) { askForBothPermissions(it) }
        collect(showPopupMenuEvent) { showPopupMenu() }
        collect(pickImageEvent) { pickImage() }
        collect(takePhotoEvent) { takePhoto() }
        collect(enterAnimationEndEvent) { this@PlantFragment.onEnterAnimationEnd() }
        collect(wateringPeriodVisibility) { setViewsVisibilities(it) }
        collect(showSnackbar) { showSnackbar(it) }
        collect(backToPlantsFragment) { actionMode?.finish() }
    }

    private fun PlantViewModel.collectPlantFlows() {
        collect(plantImage) { if (it.path.isNotEmpty()) loadAndSetPickedImage(it) }
        collect(plantName) { setEditText(binding.plantNameEditText, it) }
        collect(plantDescription) { setEditText(binding.plantDescriptionEditText, it) }
        collect(plantWateringPeriod) {
            setEditText(binding.wateringPeriodEditText, it)
            setCalendarText(it)
        }
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

    private fun getFadeOutRepeatAnimation(action: () -> Unit) = getAnimation(
        requireContext(),
        R.anim.fade_out_repeat,
        onAnimationRepeat = action
    )

    private fun getFadeOutAnimation(action: () -> Unit) = getAnimation(
        requireContext(),
        R.anim.fade_out,
        onAnimationEnd = action
    )

    private fun getFadeInAnimation(action: () -> Unit) = getAnimation(
        requireContext(),
        R.anim.fade_in,
        onAnimationEnd = action
    )

    private fun retrieveResourceId() = TypedValue().apply {
        requireContext().theme.resolveAttribute(
            R.attr.homeAsUpIndicator,
            this,
            true
        )
    }.resourceId

    private fun clearAndInflateMenu(@MenuRes menuLayout: Int) {
        actionMode?.menu?.clear()
        actionMode?.menuInflater?.inflate(menuLayout, actionMode?.menu)
    }

    private fun startActionModeCloseButtonAnimation(@DrawableRes closeButtonDrawableId: Int) {
        actionModeCloseButton.startAnimation(getFadeOutRepeatAnimation {
            actionModeCloseButton.setImageResource(closeButtonDrawableId)
        })
    }

    private fun startActionModeTitleAnimation(title: CharSequence) {
        actionModeTitle.startAnimation(getFadeOutRepeatAnimation {
            actionModeTitle.text = title
        })
    }

    private fun setContextualActionBarAnimations(
        @DrawableRes closeButtonDrawableId: Int,
        title: CharSequence,
        menuItemAnimation: (MenuItem) -> Animation
    ) {
        val menuItem = actionMode?.menu?.findItem(R.id.update)
        menuItem?.setActionView(R.layout.action_bar_update)
        menuItem?.actionView?.startAnimation(menuItemAnimation(menuItem))

        if (::actionModeCloseButton.isInitialized)
            startActionModeCloseButtonAnimation(closeButtonDrawableId)
        else {
            actionModeCloseButton = activity.findViewById(R.id.action_mode_close_button)
            startActionModeCloseButtonAnimation(closeButtonDrawableId)
        }

        if (::actionModeTitle.isInitialized) startActionModeTitleAnimation(title)
        else {
            actionModeTitle = activity.findViewById(R.id.action_bar_title)
            startActionModeTitleAnimation(title)
        }
    }

    private fun adjustToEditingContextualActionBar() {
        clearAndInflateMenu(R.menu.app_bar_plant_editing)
        setContextualActionBarAnimations(
            R.drawable.ic_close,
            getText(R.string.edit_plant)
        ) { getFadeInAnimation { it.actionView = null } }
    }

    private fun adjustToViewingContextualActionbar(plantName: String) {
        setContextualActionBarAnimations(
            retrieveResourceId(),
            plantName
        ) { getFadeOutAnimation { it.actionView = null } }

        // launching a coroutine to delay for a period of shortAnimTime so that menuItem can fade
        // out before menu being cleared
        lifecycleScope.launchWhenStarted {
            delay(resources.getInteger(R.integer.shortAnimTime).toLong())
            clearAndInflateMenu(R.menu.app_bar_plant_viewing)
        }
    }

    private fun setBottomNavigationVisibility(visibility: Int) {
        activity.setBottomNavigationVisibility(visibility)
    }

    private fun triggerAddingContextualActionBar() {
        activity.triggerContextualActionBar(
            R.menu.app_bar_plant_adding,
            R.drawable.ic_close,
            getText(R.string.add_plant)
        )
    }

    private fun triggerViewingContextualActionBar(plantName: String) {
        activity.triggerContextualActionBar(
            R.menu.app_bar_plant_viewing,
            retrieveResourceId(),
            plantName
        )
    }

    private fun WateringPlantsActivity.triggerContextualActionBar(
        @MenuRes menuResource: Int,
        @DrawableRes actionBarCloseResource: Int,
        actionBarTitleString: CharSequence? = null
    ) {
        actionMode = startSupportActionMode(
            getActionModeCallback(
                menuResource,
                ::onCreateActionMode,
                ::onPrepareActionMode,
                ::processClickedActionItem,
                ::onDestroyActionMode
            )
        )?.apply {
            title = actionBarTitleString
            activity.findViewById<AppCompatImageView>(R.id.action_mode_close_button).apply {
                setImageResource(actionBarCloseResource)
                setOnClickListener { onClick(::finish) }
            }
        }
    }

    private fun onClick(action: () -> Unit) {
        val previousState = plantViewModel.plantState.value.second
        if (previousState == null || previousState == PlantState.EDITING) action()
        else plantViewModel.confirmReverse()
    }

    private fun onDestroyActionMode() {
        activity.onBackPressed()
    }

    private fun onCreateActionMode(
        @MenuRes menuResource: Int,
        menu: Menu?
    ): Boolean {
        activity.menuInflater.inflate(menuResource, menu)
        return true
    }

    private fun onPrepareActionMode() = false

    private fun processClickedActionItem(item: MenuItem?): Boolean {
        return plantViewModel.processClickedActionIten(item?.itemId)
    }

    private fun confirmChangesReversal() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_title)
            .setMessage(R.string.dialog_message)
            .setNegativeButton(R.string.dialog_denial) { dialog, _ ->
                plantViewModel.onNoClicked()
                dialog.dismiss()
            }
            .setPositiveButton(R.string.dialog_confirmation) { dialog, _ ->
                plantViewModel.onYesClicked()
                dialog.dismiss()
            }
            .show()
    }

    private fun onEnterAnimationEnd() {
        // TODO enable views
    }
    //endregion
}