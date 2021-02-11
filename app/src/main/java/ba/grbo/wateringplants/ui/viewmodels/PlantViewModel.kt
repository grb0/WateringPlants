package ba.grbo.wateringplants.ui.viewmodels

import android.Manifest
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ba.grbo.wateringplants.R
import ba.grbo.wateringplants.data.Image
import ba.grbo.wateringplants.data.Plant
import ba.grbo.wateringplants.data.source.PlantsRepository
import ba.grbo.wateringplants.util.SharedStateLikeFlow
import ba.grbo.wateringplants.util.SingleSharedFlow
import ba.grbo.wateringplants.util.value
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlantViewModel @Inject constructor(
    private val repository: PlantsRepository
) : ViewModel() {
    //region Properties
    private var plant = Plant()

    private val readExternalStoragePermission = Manifest.permission.READ_EXTERNAL_STORAGE
    private val cameraPermission = Manifest.permission.CAMERA
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

    private val _triggerContextualActionBar = SharedStateLikeFlow<Pair<@StringRes Int, Boolean>>()
    val triggerContextualActionBar = _triggerContextualActionBar.distinctUntilChanged()

    private val _showImageLoadingProgressEvent = SharedStateLikeFlow<Boolean>()
    val showImageLoadingProgressEvent = _showImageLoadingProgressEvent.distinctUntilChanged()

    private val _removeCurrentImageEvent = MutableStateFlow<Unit?>(null)
    val removeCurrentImageEvent: StateFlow<Unit?>
        get() = _removeCurrentImageEvent

    // Single Events
    private val _showPickImageTakePhoto = MutableStateFlow(true)
    val showPickImageTakePhoto: StateFlow<Boolean>
        get() = _showPickImageTakePhoto

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

    val plantName: StateFlow<String>
        get() = plant._name

    val plantDescription: StateFlow<String>
        get() = plant._description

    val plantWateringPeriod: StateFlow<String>
        get() = plant._wateringPeriod

    val plantImage: StateFlow<Image>
        get() = plant._image

    private val _showSnackBar = SingleSharedFlow<@StringRes Int>()
    val showSnackbar: SharedFlow<Int>
        get() = _showSnackBar

    private val _backToPlantsFragment = SingleSharedFlow<Unit>()
    val backToPlantsFragment: SharedFlow<Unit>
        get() = _backToPlantsFragment
    //endregion

    // region Static values
    companion object {
        private const val rotateRight = 90f
        private const val rotateLeft = -90f

    }
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
        if (isReadExternalStoragePermissionGranted && isCameraPermissionGranted) {
            _takePhotoEvent.tryEmit(Unit)
        } else _checkIfBothPermissionsWereAlreadyGiven.tryEmit(Unit)
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
                arrayOf(readExternalStoragePermission, cameraPermission)
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

    fun onImagePathProvided(pickedImagePath: String?) {
        pickedImagePath?.let {
            if (pickedImagePath != plant.image.path) {
                rotationAngle = 0f // New image picked, reset rotationAngle
                if (plant.image.path.isNotEmpty()) _removeCurrentImageEvent.value = Unit
                _showPickImageTakePhoto.tryEmit(false)
                plant.image = plant.image.copy(path = pickedImagePath)
            }
        }
    }

    fun onTakeImageResultArrival(wasTaken: Boolean, takenPhotoPath: String?) {
        takenPhotoPath?.let {
            if (wasTaken) onImagePathProvided(takenPhotoPath)
            // TODO else notify user of error while taking picture
        }
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
        if (!isPlantImagePathEmpty()) {
            _showImageLoadingProgressEvent.value?.let { if (!it) _showPopupMenuEvent.tryEmit(Unit) }
        }
        return true
    }

    private fun onRotateImageClick(rotationAngle: Float) {
        val totalRotationAngle = plant.image.rotationAngle + rotationAngle
        plant.image = plant.image.copy(rotationAngle = totalRotationAngle)
    }

    private fun onRotateImageRightClick() {
        onRotateImageClick(rotateRight)
    }

    private fun onRotateImageLeftClick() {
        onRotateImageClick(rotateLeft)
    }

    fun setWateringPeriodVisibility(visibility: Boolean) {
        if ((visibility && !_wateringPeriodVisibility.value) || (!visibility && _wateringPeriodVisibility.value)) {
            _wateringPeriodVisibility.value = visibility
        }
    }

    fun setPlantName(text: String) {
        if (text != plant.name) plant.name = text
    }

    fun setPlantDescription(text: String) {
        if (text != plant.description) plant.description = text
    }

    fun setWateringPeriod(text: String) {
        if (text != plant.wateringPeriod) plant.wateringPeriod = text
    }

    fun onCreatePlantClicked(): Boolean {
        if (shouldSavePlant()) viewModelScope.launch(Dispatchers.IO) {
            repository.insertPlant(plant)
            notifyUserOfSuccessfulInsert()
            goBackToPlantsFragment()
        } else notifyUserOfIncompletePlant()
        return true
    }

    private fun isPlantNameEmpty() = plant.name.isEmpty()
    private fun isPlantDescriptionEmpty() = plant.description.isEmpty()
    private fun isPlantImagePathEmpty() = plant.image.path.isEmpty()

    private fun shouldSavePlant(): Boolean {
        return !isPlantNameEmpty() && !isPlantDescriptionEmpty() && !isPlantImagePathEmpty()
    }

    private fun notifyUserOfIncompletePlant() {
        val isPlantNameEmpty = isPlantNameEmpty()
        val isPlantDescriptionEmpty = isPlantDescriptionEmpty()
        val isPlantImagePathEmpty = isPlantImagePathEmpty()

        when {
            isPlantNameEmpty && !isPlantDescriptionEmpty && !isPlantImagePathEmpty -> {
                notifyUserOfEmptyPlantName()
            }
            isPlantNameEmpty && isPlantDescriptionEmpty && !isPlantImagePathEmpty -> {
                notifyUserOfEmptyPlantNameAndDescription()
            }
            isPlantNameEmpty && !isPlantDescriptionEmpty && isPlantImagePathEmpty -> {
                notifyUserOfEmptyePlantNameAndImagePath()
            }
            !isPlantNameEmpty && isPlantDescriptionEmpty && !isPlantImagePathEmpty -> {
                notifyUserOfEmptyPlantDescription()
            }
            !isPlantNameEmpty && isPlantDescriptionEmpty && isPlantImagePathEmpty -> {
                notifyUserOfEmptyPlantDescriptionAndImagePath()
            }
            !isPlantNameEmpty && !isPlantDescriptionEmpty && isPlantImagePathEmpty -> {
                notifyUserOfEmptyPlantImagePath()
            }
            isPlantNameEmpty && isPlantDescriptionEmpty && isPlantImagePathEmpty -> {
                notifyUserOfCompleteEmptiness()
            }
        }
    }

    private fun notifyUserOfEmptyPlantName() {
        _showSnackBar.tryEmit(R.string.plant_name_empty)
    }

    private fun notifyUserOfEmptyPlantNameAndDescription() {
        _showSnackBar.tryEmit(R.string.plant_name_and_description_empty)
    }

    private fun notifyUserOfEmptyePlantNameAndImagePath() {
        _showSnackBar.tryEmit(R.string.plant_name_and_image_path_empty)
    }

    private fun notifyUserOfEmptyPlantDescription() {
        _showSnackBar.tryEmit(R.string.plant_description_empty)
    }

    private fun notifyUserOfEmptyPlantDescriptionAndImagePath() {
        _showSnackBar.tryEmit(R.string.plant_description_and_image_path_empty)
    }

    private fun notifyUserOfEmptyPlantImagePath() {
        _showSnackBar.tryEmit(R.string.plant_image_path_empty)
    }

    private fun notifyUserOfCompleteEmptiness() {
        _showSnackBar.tryEmit(R.string.plant_complete_empty)
    }

    private fun notifyUserOfSuccessfulInsert() {
        _showSnackBar.tryEmit(R.string.plant_successfully_inserted)
    }

    private fun goBackToPlantsFragment() {
        _backToPlantsFragment.tryEmit(Unit)
    }

    fun onImageFailedToLoad() {
        _showImageLoadingProgressEvent.tryEmit(false)
        _showSnackBar.tryEmit(R.string.plant_img_failed_to_load)
    }

    fun onImageLoadedSuccessfully() {
        _showImageLoadingProgressEvent.tryEmit(false)
    }

    fun showImageLoadingProgressEvent() {
        _showImageLoadingProgressEvent.tryEmit(true)
    }
    //endregion
}