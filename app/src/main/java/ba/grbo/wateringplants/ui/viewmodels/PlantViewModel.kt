package ba.grbo.wateringplants.ui.viewmodels

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.lifecycle.*
import ba.grbo.wateringplants.R
import ba.grbo.wateringplants.data.Plant
import ba.grbo.wateringplants.data.source.PlantsRepository
import ba.grbo.wateringplants.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class PlantViewModel @Inject constructor(
    private val repository: PlantsRepository
) : ViewModel() {
    //region Properties
    val plant = Plant(
        MutableStateFlow("Forest oak"),
        flowOf("Forest oaks are found in forests."),
        flowOf("1"),
    )

    private val readExternalStoragePermission = Manifest.permission.READ_EXTERNAL_STORAGE
    private val cameraPermission = Manifest.permission.CAMERA
    private var isReadExternalStoragePermissionGranted = false
    private var isCameraPermissionGranted = false
    private var realUriPath: String? = null
    private var rotationAngle = 0f
        private set(value) {
            field = when (value) {
                270f -> -90f
                -270f -> 90f
                else -> value
            }
        }

    private val _triggerContextualActionBar = SharedStateLikeFlow<Pair<@StringRes Int, Boolean>>()
    val triggerContextualActionBar = _triggerContextualActionBar.distinctUntilChanged()

    private val _plantImage = SharedStateLikeFlow<Bitmap>()
    val plantImage = _plantImage.distinctUntilChanged()

    private val _showImageLoadingProgressEvent = SharedStateLikeFlow<Boolean>()
    val showImageLoadingProgressEvent = _showImageLoadingProgressEvent.distinctUntilChanged()

    private val _removeCurrentImageEvent = MutableStateFlow<Unit?>(null)
    val removeCurrentImageEvent: StateFlow<Unit?>
        get() = _removeCurrentImageEvent

    // Single Events
    private val _requestPickedImageDependenciesEvent = SingleSharedFlow<Uri?>()
    val requestPickedImageDependenciesEvent: SharedFlow<Uri?>
        get() = _requestPickedImageDependenciesEvent

    private val _requestTakenPhotoDependenciesEvent = SingleSharedFlow<Unit>()
    val requestTakenPhotoDependenciesEvent: SharedFlow<Unit>
        get() = _requestTakenPhotoDependenciesEvent

    private val _showPickImageTakePhoto = MutableStateFlow(true)
    val showPickImageTakePhoto: StateFlow<Boolean>
        get() = _showPickImageTakePhoto

    init {
        _requestPickedImageDependenciesEvent
            .onEach { _showPickImageTakePhoto.value = it == null }
            .launchIn(viewModelScope)

        _requestTakenPhotoDependenciesEvent
            .onEach { _showPickImageTakePhoto.value = it != Unit }
            .launchIn(viewModelScope)
    }

    private val _checkIfReadExternalStoragePermissionWasAlreadyGiven = SingleSharedFlow<Unit>()
    val checkIfReadExternalStoragePermissionWasAlreadyGiven: SharedFlow<Unit>
        get() = _checkIfReadExternalStoragePermissionWasAlreadyGiven

    private val _checkIfBothPermissionsWereAlreadyGiven = SingleSharedFlow<Unit>()
    val checkIfBothPermissionsWereAlreadyGiven: SharedFlow<Unit>
        get() = _checkIfBothPermissionsWereAlreadyGiven

    private val _askForReadExternalStoragePermissionEvent = SingleSharedFlow<String>()
    val askForReadExternalStoragePermissionEvent: SharedFlow<String>
        get() = _askForReadExternalStoragePermissionEvent

    private val _askForCameraPermissionEvent = SingleSharedFlow<String>()
    val askForCameraPermissionEvent: SharedFlow<String>
        get() = _askForCameraPermissionEvent

    private val _askForBothPermissionsEvent = SingleSharedFlow<Array<String>>()
    val askForBothPermissionsEvent: SharedFlow<Array<String>>
        get() = _askForBothPermissionsEvent

    private val _showPopupMenuEvent = SingleSharedFlow<Unit>()
    val showPopupMenuEvent: SharedFlow<Unit>
        get() = _showPopupMenuEvent

    private val _pickImageEvent = SingleSharedFlow<Unit>()
    val pickImageEvent: SharedFlow<Unit>
        get() = _pickImageEvent

    private val _takePhotoEvent = SingleSharedFlow<Unit>()
    val takePhotoEvent: SharedFlow<Unit>
        get() = _takePhotoEvent

    private val _enterAnimationEndEvent = SingleSharedFlow<Unit>()
    val enterAnimationEndEvent: SharedFlow<Unit>
        get() = _enterAnimationEndEvent

    private val _wateringPeriodVisibility = MutableStateFlow(true)
    val wateringPeriodVisibility: StateFlow<Boolean>
        get() = _wateringPeriodVisibility

    private val _wateringPeriodText = MutableStateFlow("1")
    val wateringPeriodText: StateFlow<String>
        get() = _wateringPeriodText
    //endregion

    //region Helper methods
    fun onEnterAnimationStart() {
        _triggerContextualActionBar.tryEmit(R.string.add_plant to false)
    }

    fun onEnterAnimationEnd() {
        _enterAnimationEndEvent.tryEmit(Unit)
    }

    fun onCalendarImgClick() {
        _wateringPeriodVisibility.value = false
    }

    fun onPickImageClick() {
        if (isReadExternalStoragePermissionGranted) _pickImageEvent.tryEmit(Unit)
        else _checkIfReadExternalStoragePermissionWasAlreadyGiven.tryEmit(Unit)
    }

    fun onTakePhotoClick() {
        if (isReadExternalStoragePermissionGranted && isCameraPermissionGranted)
            _takePhotoEvent.tryEmit(Unit)
        else _checkIfBothPermissionsWereAlreadyGiven.tryEmit(Unit)
    }

    fun onWasReadExternalStoragePermissionWasAlreadyGivenResultArrival(wasGranted: Boolean) {
        isReadExternalStoragePermissionGranted = wasGranted
        if (isReadExternalStoragePermissionGranted) _pickImageEvent.tryEmit(Unit)
        else _askForReadExternalStoragePermissionEvent.tryEmit(readExternalStoragePermission)
    }

    fun onWereBothPermissionsAlreadyGivenResultArrival(wereGranted: Pair<Boolean, Boolean>) {
        isReadExternalStoragePermissionGranted = wereGranted.first
        isCameraPermissionGranted = wereGranted.second

        if (isReadExternalStoragePermissionGranted && isCameraPermissionGranted)
            _takePhotoEvent.tryEmit(Unit)
        else if (!isReadExternalStoragePermissionGranted && !isCameraPermissionGranted)
            _askForBothPermissionsEvent.tryEmit(
                arrayOf(
                    readExternalStoragePermission,
                    cameraPermission
                )
            )
        else if (isReadExternalStoragePermissionGranted && !isCameraPermissionGranted)
            _askForCameraPermissionEvent.tryEmit(cameraPermission)
    }

    fun onAskForReadExternalStoragePermissionResultArrival(isGranted: Boolean) {
        isReadExternalStoragePermissionGranted = isGranted
        if (isReadExternalStoragePermissionGranted) _pickImageEvent.tryEmit(Unit)
        // TODO else show snackbar saying that permission was denied
    }

    fun onAskForCameraPermissionResultArrival(isGranted: Boolean) {
        isCameraPermissionGranted = isGranted
        if (isCameraPermissionGranted) _takePhotoEvent.tryEmit(Unit)
        // TODO else show snackbar saying that permission was denied
    }

    fun onAskForBothPermissionsResultArival(wereGranted: Map<String, Boolean>) {
        isReadExternalStoragePermissionGranted = wereGranted[readExternalStoragePermission] ?: false
        isCameraPermissionGranted = wereGranted[cameraPermission] ?: false

        if (isReadExternalStoragePermissionGranted && isCameraPermissionGranted)
            _takePhotoEvent.tryEmit(Unit)
        // TODO else notify user which permission(s) was (were) denied
    }

    fun onImageUriArrival(uri: Uri?, realUriPath: String?) {
        uri?.let {
            if (this.realUriPath != realUriPath) {
                rotationAngle = 0f // New image picked, reset rotationAngle
                if (_plantImage.value != null) _removeCurrentImageEvent.value = Unit
                _showImageLoadingProgressEvent.tryEmit(true)
                _requestPickedImageDependenciesEvent.tryEmit(Uri.fromFile(File(realUriPath!!)))

            }
        }
    }

    fun onTakeImageResultArrival(wasTaken: Boolean) {
        if (wasTaken) _requestTakenPhotoDependenciesEvent.tryEmit(Unit)
        // TODO else notify user of error while taking picture
    }

    private fun onPlantImageUriNotValidAnymore() {
        _requestPickedImageDependenciesEvent.tryEmit(null)
    }

    fun onEditTextReleaseFocus() {
        _wateringPeriodVisibility.value = true
    }

    fun plantFragmentConstraintLayoutOnFocusChange(hasFocus: Boolean) {
        if (hasFocus && !_wateringPeriodVisibility.value)
            _wateringPeriodVisibility.value = true
    }

    fun processPopupMenuItemId(@IdRes itemId: Int): Boolean {
        when (itemId) {
            R.id.pick_img_popup -> onPickImageClick()
            R.id.take_photo_popup -> onTakePhotoClick()
            R.id.rotate_img_right_popup -> onRotateImageRightClick()
            R.id.rotate_img_left_popup -> onRotateImageLeftClick()
        }
        return true
    }

    fun onPlantImageLongClick(): Boolean {
        _plantImage.value?.run {
            _showImageLoadingProgressEvent.value?.let {
                if (!it) _showPopupMenuEvent.tryEmit(Unit)
            }
        }
        return true
    }

    private fun onBitmapLoaded(bitmap: Bitmap) {
        if (rotationAngle.hasToBeRotated) onPlantImageRotate(rotationAngle)
        else {
            _showImageLoadingProgressEvent.tryEmit(false)
            _removeCurrentImageEvent.value = null
            _plantImage.tryEmit(bitmap)
        }
    }

    private fun onRotateImageRightClick() {
        onPlantImageRotate(90f)
    }

    private fun onRotateImageLeftClick() {
        onPlantImageRotate(-90f)
    }

    private fun onPlantImageRotate(rotationAngle: Float) {
        _plantImage.value?.let {
            viewModelScope.launch {
                this@PlantViewModel.rotationAngle += rotationAngle
                _plantImage.tryEmit(rotateImage(it, rotationAngle))
            }
        }
    }

    fun onImageDependenciesProvided(
        width: Int,
        height: Int,
        streams: Array<InputStream?>,
        realUriPath: String?
    ) {
        this.realUriPath = realUriPath
        viewModelScope.launch {
            try {
                val bitmap = decodeSampledBitmapFromUri(
                    streams,
                    width,
                    height,
                    coroutineContext + Dispatchers.IO
                )

                onBitmapLoaded(bitmap ?: throw IllegalArgumentException("Bitmap cannot be null"))
            } catch (e: FileNotFoundException) {
                // TODO inform user that the image was deleted
                onPlantImageUriNotValidAnymore()
            } catch (e: IllegalArgumentException) {
                // TODO inform user that the image couldn't be loaded
            }
        }
    }

    private suspend fun decodeSampledBitmapFromUri(
        streams: Array<InputStream?>,
        reqWidth: Int,
        reqHeight: Int,
        context: CoroutineContext
    ) = withContext(context) {
        // First decode with inJustDecodeBounds=true to check dimensions
        BitmapFactory.Options().run {
            inJustDecodeBounds = true
            if (isActive) BitmapFactory.decodeStream(streams[0], null, this)

            // Calculate inSampleSize
            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight, coroutineContext)

            // Decode bitmap with inSampleSize set
            inJustDecodeBounds = false

            BitmapFactory.decodeStream(streams[1], null, this)
        }
    }

    private suspend fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int,
        context: CoroutineContext
    ) = withContext(context) {
        // Raw height and width of image
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (isActive && halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        inSampleSize
    }

    private suspend fun rotateImage(image: Bitmap, angle: Float) = withContext(Dispatchers.IO) {
        val matrix = Matrix().apply { postRotate(angle) }
        Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrix, true)
    }

    fun setWateringPeriodVisibility(visibility: Boolean) {
        if ((visibility && !_wateringPeriodVisibility.value) || (!visibility && _wateringPeriodVisibility.value)) {
            _wateringPeriodVisibility.value = visibility
        }
    }

    fun setWateringPeriodText(text: String) {
        if (text != _wateringPeriodText.value) _wateringPeriodText.value = text
    }
    //endregion
}