package ba.grbo.wateringplants.ui.fragments

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.drawable.InsetDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import android.util.TypedValue
import android.view.*
import android.view.animation.Animation
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.ActionMode
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import ba.grbo.wateringplants.R
import ba.grbo.wateringplants.databinding.FragmentPlantBinding
import ba.grbo.wateringplants.ui.activities.WateringPlantsActivity
import ba.grbo.wateringplants.ui.viewmodels.PlantViewModel
import ba.grbo.wateringplants.util.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*


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
        plantViewModel.observeEvents()
        binding = FragmentPlantBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = plantViewModel

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

            plantImg.setOnLongClickListener { plantViewModel.onPlantImageLongClick() }
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
    private fun PlantViewModel.observeEvents() {
        observeLiveData(triggerContextualActionBar, viewLifecycleOwner) {
            activity.triggerContextualActionBar()
        }
        observeLiveData(plantImage, viewLifecycleOwner) { setBitmapToImageView(it) }
        observeLiveData(showImageLoadingProgressEvent, viewLifecycleOwner) {
            showImageLoadingProgress(it.toVisibility)
        }
        observeLiveData(removeCurrentImageEvent, viewLifecycleOwner) {
            it?.run { removeCurrentImage() }
        }
        observeEvent(requestPickedImageDependenciesEvent, viewLifecycleOwner) {
            it?.also { providePickedImageDependencies(it) }
        }

        observeEvent(requestTakenPhotoDependenciesEvent, viewLifecycleOwner) {
            provideTakenPhotoDependencies()
        }

        observeEvent(checkIfReadExternalStoragePermissionWasAlreadyGiven, viewLifecycleOwner) {
            wasReadExternalStoragePermissionAlreadyGranted()
        }
        observeEvent(checkIfBothPermissionsWereAlreadyGiven, viewLifecycleOwner) {
            wereBothPermissionsAlreadyGivenResultArrival()
        }
        observeEvent(askForReadExternalStoragePermissionEvent, viewLifecycleOwner) {
            askForReadExternalStoragePermission(it)
        }
        observeEvent(askForCameraPermissionEvent, viewLifecycleOwner) { askForCameraPermission(it) }
        observeEvent(askForBothPermissionsEvent, viewLifecycleOwner) { askForBothPermissions(it) }
        observeEvent(showPopupMenuEvent, viewLifecycleOwner) { showPopupMenu() }
        observeEvent(pickImageEvent, viewLifecycleOwner) { pickImage() }
        observeEvent(takePhotoEvent, viewLifecycleOwner) { takePhoto() }
        observeEvent(enterAnimationEndEvent, viewLifecycleOwner) {
            this@PlantFragment.onEnterAnimationEnd()
        }
    }
    //endregion

    //region Helper methods
    @Suppress("RestrictedApi")
    private fun showPopupMenu() {
        popupMenu.show()
    }

    private fun showImageLoadingProgress(visibility: Int) {
        binding.imgLoadingProgress.visibility = visibility
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

    private fun Uri.getRealPathFromUriAPI19(context: Context): String? {
        if (DocumentsContract.isDocumentUri(context, this)) { // DownloadsProvider
            if (isDownloadsDocument(this)) { // DownloadsProvider
                val id = DocumentsContract.getDocumentId(this)
                if (!TextUtils.isEmpty(id)) {
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:", "")
                    }
                    return try {
                        val contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"),
                                java.lang.Long.valueOf(id)
                        )

                        getDataColumn(context, contentUri, null, null)
                    } catch (e: NumberFormatException) {
                        null
                    }
                }
            } else if (isMediaDocument(this)) { // MediaProvider
                val docId = DocumentsContract.getDocumentId(this)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                val contentUri = when (split[0]) {
                    "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    else -> null
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])

                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(this.scheme, ignoreCase = true)) { // MediaStore (and general)
            return if (isGooglePhotosUri(this)) this.lastPathSegment
            else getDataColumn(context, this, null, null)
        } else if ("file".equals(this.scheme, ignoreCase = true)) { // File
            return this.path
        }


        return null
    }

    private fun getDataColumn(
            context: Context,
            uri: Uri?,
            selection: String?,
            selectionArgs: Array<String>?
    ): String? {

        var cursor: Cursor? = null
        val column = "_data"

        try {
            cursor =
                    context.contentResolver.query(
                            uri!!,
                            arrayOf(column),
                            selection,
                            selectionArgs,
                            null
                    )
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }
    //endregion
}