package ba.grbo.wateringplants.ui.viewmodels

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.annotation.IdRes
import androidx.lifecycle.*
import ba.grbo.wateringplants.R
import ba.grbo.wateringplants.util.Event
import ba.grbo.wateringplants.util.combineWith
import ba.grbo.wateringplants.util.hasToBeRotated
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.InputStream
import kotlin.coroutines.CoroutineContext

class PlantViewModel : ViewModel() {
    //region Properties
    private val readExternalStoragePermission = Manifest.permission.READ_EXTERNAL_STORAGE
    private val cameraPermission = Manifest.permission.CAMERA

    private val _triggerContextualActionBar = MutableLiveData<Boolean>()
    val triggerContextualActionBar: LiveData<Boolean>
        get() = _triggerContextualActionBar

    private val _enterAnimationEndEvent = MutableLiveData<Event<Unit>>()
    val enterAnimationEndEvent: LiveData<Event<Unit>>
        get() = _enterAnimationEndEvent

    private val _checkIfReadExternalStoragePermissionWasAlreadyGiven =
            MutableLiveData<Event<Unit>>()
    val checkIfReadExternalStoragePermissionWasAlreadyGiven: LiveData<Event<Unit>>
        get() = _checkIfReadExternalStoragePermissionWasAlreadyGiven

    private val _checkIfBothPermissionsWereAlreadyGiven = MutableLiveData<Event<Unit>>()
    val checkIfBothPermissionsWereAlreadyGiven: LiveData<Event<Unit>>
        get() = _checkIfBothPermissionsWereAlreadyGiven

    private val _askForReadExternalStoragePermissionEvent = MutableLiveData<Event<String>>()
    val askForReadExternalStoragePermissionEvent: LiveData<Event<String>>
        get() = _askForReadExternalStoragePermissionEvent

    private val _askForCameraPermissionEvent = MutableLiveData<Event<String>>()
    val askForCameraPermissionEvent: LiveData<Event<String>>
        get() = _askForCameraPermissionEvent

    private val _askForBothPermissionsEvent = MutableLiveData<Event<Array<String>>>()
    val askForBothPermissionsEvent: LiveData<Event<Array<String>>>
        get() = _askForBothPermissionsEvent

    private val _pickImageEvent = MutableLiveData<Event<Unit>>()
    val pickImageEvent: LiveData<Event<Unit>>
        get() = _pickImageEvent

    private val _takePhotoEvent = MutableLiveData<Event<Unit>>()
    val takePhotoEvent: LiveData<Event<Unit>>
        get() = _takePhotoEvent

    private val _requestPickedImageDependenciesEvent = MutableLiveData<Event<Uri?>>()
    val requestPickedImageDependenciesEvent: LiveData<Event<Uri?>>
        get() = _requestPickedImageDependenciesEvent

    private val _requestTakenPhotoDependenciesEvent = MutableLiveData<Event<Unit>>()
    val requestTakenPhotoDependenciesEvent: LiveData<Event<Unit>>
        get() = _requestTakenPhotoDependenciesEvent

    private val _showPopupMenuEvent = MutableLiveData<Event<Unit>>()
    val showPopupMenuEvent: LiveData<Event<Unit>>
        get() = _showPopupMenuEvent

    private val _plantImage = MutableLiveData<Bitmap>()
    val plantImage: LiveData<Bitmap>
        get() = _plantImage

    private val _showImageLoadingProgressEvent = MutableLiveData<Boolean>()
    val showImageLoadingProgressEvent: LiveData<Boolean>
        get() = _showImageLoadingProgressEvent

    private val _removeCurrentImageEvent = MutableLiveData<Boolean?>()
    val removeCurrentImageEvent: LiveData<Boolean?>
        get() = _removeCurrentImageEvent

    private var isReadExternalStoragePermissionGranted = false
    private var isCameraPermissionGranted = false

    private var rotationAngle = 0f
        private set(value) {
            field = when (value) {
                270f -> -90f
                -270f -> 90f
                else -> value
            }
        }

    private var realUriPath: String? = null

    val isPlantImageAvailable = _requestPickedImageDependenciesEvent.combineWith(
            _requestTakenPhotoDependenciesEvent
    ) { pickedImageDependenciesEvent, takenPhotoDependenciesEvent ->
        pickedImageDependenciesEvent != null || takenPhotoDependenciesEvent != null
    }

    // first value represents text, second one visibility
    val wateringPeriod = MutableLiveData("1") to MutableLiveData(true)
    //endregion

    //region Helper methods
    fun onEnterAnimationStart() {
        _triggerContextualActionBar.value = true
    }

    fun onEnterAnimationEnd() {
        _enterAnimationEndEvent.value = Event(Unit)
    }

    fun onCalendarImgClick() {
        wateringPeriod.second.value = false
    }

    fun onPickImageClick() {
        if (isReadExternalStoragePermissionGranted) _pickImageEvent.value = Event(Unit)
        else _checkIfReadExternalStoragePermissionWasAlreadyGiven.value = Event(Unit)
    }

    fun onTakePhotoClick() {
        if (isReadExternalStoragePermissionGranted && isCameraPermissionGranted)
            _takePhotoEvent.value = Event(Unit)
        else _checkIfBothPermissionsWereAlreadyGiven.value = Event(Unit)
    }

    fun onWasReadExternalStoragePermissionWasAlreadyGivenResultArrival(wasGranted: Boolean) {
        isReadExternalStoragePermissionGranted = wasGranted
        if (isReadExternalStoragePermissionGranted) _pickImageEvent.value = Event(Unit)
        else _askForReadExternalStoragePermissionEvent.value = Event(readExternalStoragePermission)
    }

    fun onWereBothPermissionsAlreadyGivenResultArrival(wereGranted: Pair<Boolean, Boolean>) {
        isReadExternalStoragePermissionGranted = wereGranted.first
        isCameraPermissionGranted = wereGranted.second

        if (isReadExternalStoragePermissionGranted && isCameraPermissionGranted)
            _takePhotoEvent.value = Event(Unit)
        else if (!isReadExternalStoragePermissionGranted && !isCameraPermissionGranted)
            _askForBothPermissionsEvent.value = Event(
                    arrayOf(
                            readExternalStoragePermission,
                            cameraPermission
                    )
            )
        else if (isReadExternalStoragePermissionGranted && !isCameraPermissionGranted)
            _askForCameraPermissionEvent.value = Event(cameraPermission)
    }

    fun onAskForReadExternalStoragePermissionResultArrival(isGranted: Boolean) {
        isReadExternalStoragePermissionGranted = isGranted
        if (isReadExternalStoragePermissionGranted) _pickImageEvent.value = Event(Unit)
        // TODO else show snackbar saying that permission was denied
    }

    fun onAskForCameraPermissionResultArrival(isGranted: Boolean) {
        isCameraPermissionGranted = isGranted
        if (isCameraPermissionGranted) _takePhotoEvent.value = Event(Unit)
        // TODO else show snackbar saying that permission was denied
    }

    fun onAskForBothPermissionsResultArival(wereGranted: Map<String, Boolean>) {
        isReadExternalStoragePermissionGranted = wereGranted[readExternalStoragePermission] ?: false
        isCameraPermissionGranted = wereGranted[cameraPermission] ?: false

        if (isReadExternalStoragePermissionGranted && isCameraPermissionGranted)
            _takePhotoEvent.value = Event(Unit)
        // TODO else notify user which permission(s) was (were) denied
    }

    fun onImageUriArrival(uri: Uri?, realUriPath: String?) {
        uri?.let {
            if (this.realUriPath != realUriPath) {
                rotationAngle = 0f // New image picked, reset rotationAngle
                if (_plantImage.value != null) _removeCurrentImageEvent.value = true
                _showImageLoadingProgressEvent.value = true
                _requestPickedImageDependenciesEvent.value = Event(it)
            }
        }
    }

    fun onTakeImageResultArrival(wasTaken: Boolean) {
        if (wasTaken) _requestTakenPhotoDependenciesEvent.value = Event(Unit)
        // TODO else notify user of error while taking picture
    }

    private fun onPlantImageUriNotValidAnymore() {
        _requestPickedImageDependenciesEvent.value = Event(null)
    }

    fun onEditTextReleaseFocus() {
        wateringPeriod.second.value = true
    }

    fun plantFragmentConstraintLayoutOnFocusChange(hasFocus: Boolean) {
        if (hasFocus && wateringPeriod.second.value == false)
            wateringPeriod.second.value = true
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
            _showImageLoadingProgressEvent.value?.let { if (!it) _showPopupMenuEvent.value = Event(Unit) }
        }
        return true
    }

    private fun onBitmapLoaded(bitmap: Bitmap) {
        if (rotationAngle.hasToBeRotated) onPlantImageRotate(rotationAngle)
        else {
            _showImageLoadingProgressEvent.value = false
            _removeCurrentImageEvent.value = null
            _plantImage.value = bitmap
        }
    }

    private fun onRotateImageRightClick() {
        onPlantImageRotate(90f)
    }

    private fun onRotateImageLeftClick() {
        onPlantImageRotate(-90f)
    }

    private fun onPlantImageRotate(rotationAngle: Float) {
        plantImage.value?.let {
            viewModelScope.launch {
                this@PlantViewModel.rotationAngle += rotationAngle
                _plantImage.value = rotateImage(it, rotationAngle)
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = decodeSampledBitmapFromUri(
                        streams,
                        width,
                        height,
                        coroutineContext
                )

                withContext(Dispatchers.Main) {
                    onBitmapLoaded(
                            bitmap ?: throw IllegalArgumentException("Bitmap cannot be null")
                    )
                }
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
    //endregion
}