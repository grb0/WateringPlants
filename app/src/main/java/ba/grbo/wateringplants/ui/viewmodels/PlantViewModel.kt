package ba.grbo.wateringplants.ui.viewmodels

import android.Manifest
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ba.grbo.wateringplants.R
import ba.grbo.wateringplants.data.Image
import ba.grbo.wateringplants.data.Plant
import ba.grbo.wateringplants.data.Result
import ba.grbo.wateringplants.data.Result.Success
import ba.grbo.wateringplants.data.source.PlantsRepository
import ba.grbo.wateringplants.ui.fragments.PlantFragmentArgs
import ba.grbo.wateringplants.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlantViewModel @Inject constructor(
    private val repository: PlantsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    //region Properties

    private val fragmentArgs = savedStateHandle.get<Bundle>(BUNDLE_ARGS)!!
    private val args = PlantFragmentArgs.fromBundle(fragmentArgs)

    private val _plantState =
        MutableStateFlow<Pair<PlantState, PlantState?>>(args.plantState to null)
    val plantState: StateFlow<Pair<PlantState, PlantState?>>
        get() = _plantState

    private val _adjustToEditingContextualActionBar = SharedStateLikeFlow<Unit>()
    val adjustToEditingContextualActionBar: SharedFlow<Unit>
        get() = _adjustToEditingContextualActionBar

    private val _adjusToViewingContextualActionBar = SharedStateLikeFlow<String>()
    val adjustToViewingContextualActionbar: SharedFlow<String>
        get() = _adjusToViewingContextualActionBar

    private val plantId = args.plantId

    private lateinit var plant: Plant
    private lateinit var unmodifiedPlant: Plant

    private val _collectPlantFlowsEvent = SharedStateLikeFlow<Unit>()
    val collectPlantFlowsEvent = _collectPlantFlowsEvent.distinctUntilChanged()

    private val _setSharedElementEnterTransitionEvent = SharedStateLikeFlow<Unit>()
    val setSharedElementEnterTransitionEvent =
        _setSharedElementEnterTransitionEvent.distinctUntilChanged()

    private val _setTransitionNameEvent = SharedStateLikeFlow<String>()
    val setTransitionNameEvent = _setTransitionNameEvent.distinctUntilChanged()

    init {
        if (_plantState.value == PlantState.ADDING to null) {
            plant = Plant()
            viewModelScope.launch { _collectPlantFlowsEvent.emit(Unit) }
        } else viewModelScope.launch(Dispatchers.IO) {
            val retrievedPlant = repository.getPlant(plantId)
            if (retrievedPlant is Success) {
                plant = retrievedPlant.data
                unmodifiedPlant = plant.clone()
                _setSharedElementEnterTransitionEvent.tryEmit(Unit)
                _setTransitionNameEvent.tryEmit(plantId.toString())
                _collectPlantFlowsEvent.emit(Unit)
                _showPickImageTakePhoto.emit(false)
            }
            // TODO else notify user of error retrievedPlant as Error -> retrievedPlant.exception
        }

        plantState.onEach {
            if (it.first == PlantState.EDITING) _adjustToEditingContextualActionBar.tryEmit(Unit)
            else if (it.first == PlantState.VIEWING && it.second == PlantState.EDITING)
                _adjusToViewingContextualActionBar.tryEmit(plant.name)
        }.launchIn(viewModelScope)
    }

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

    private val _removeBottomNavigationEvent = SharedStateLikeFlow<Int>()
    val removeBottomNavigation = _removeBottomNavigationEvent.distinctUntilChanged()

    private val _triggerAddingContextualActionBar = SharedStateLikeFlow<Unit>()
    val triggerAddingContextualActionBar = _triggerAddingContextualActionBar.distinctUntilChanged()

    private val _triggerViewingContextualActionBar = SharedStateLikeFlow<String>()
    val triggerViewingContextualActionbar =
        _triggerViewingContextualActionBar.distinctUntilChanged()

    private val _showImageLoadingProgressEvent = SharedStateLikeFlow<Boolean>()
    val showImageLoadingProgressEvent = _showImageLoadingProgressEvent.distinctUntilChanged()

    private val _removeCurrentImageEvent = MutableStateFlow<Unit?>(null)
    val removeCurrentImageEvent: StateFlow<Unit?>
        get() = _removeCurrentImageEvent

    private val _confirmReverseEvent = SharedStateLikeFlow<Boolean>()
    val confirmReverseEvent: SharedFlow<Boolean>
        get() = _confirmReverseEvent

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
        _removeBottomNavigationEvent.tryEmit(false.toVisibility)
        if (_plantState.value.first == PlantState.ADDING) _triggerAddingContextualActionBar.tryEmit(
            Unit
        ) else _triggerViewingContextualActionBar.tryEmit(plant.name)
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
                checkPlantState()
            }
        }
    }

    private fun checkPlantState() {
        if (_plantState.value.first == PlantState.VIEWING) onPlantEdited()
        else if (_plantState.value.first == PlantState.EDITING) onPlantNotEdited()
    }

    private fun isPlantEdited() = plant != unmodifiedPlant

    private fun onPlantEdited() {
        if (isPlantEdited()) _plantState.value = PlantState.EDITING to PlantState.VIEWING
    }

    private fun onPlantNotEdited() {
        if (!isPlantEdited()) _plantState.value = PlantState.VIEWING to PlantState.EDITING
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
        checkPlantState()
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

    fun setPlantName(name: String) {
        if (name != plant.name) {
            plant.name = name
            checkPlantState()
        }
    }

    fun setPlantDescription(description: String) {
        if (description != plant.description) {
            plant.description = description
            checkPlantState()
        }
    }

    fun setWateringPeriod(wateringPeriod: String) {
        if (wateringPeriod != plant.wateringPeriod) {
            plant.wateringPeriod = wateringPeriod
            checkPlantState()
        }
    }

    private fun writeToDB(
        writeToDBAction: suspend (Plant) -> Result<Boolean>,
        notifyUserSuccessfulAction: () -> Unit
    ) {
        if (shouldSavePlant()) viewModelScope.launch(Dispatchers.IO) {
            val status = writeToDBAction(plant)
            if (status is Success) {
                notifyUserSuccessfulAction()
                goBackToPlantsFragment()
            }
            // TODO notify user of error
        } else notifyUserOfIncompletePlant()
    }

    fun processClickedActionIten(@IdRes menuId: Int?): Boolean {
        menuId?.let {
            when (it) {
                R.id.create -> writeToDB(repository::insertPlant, ::notifyUserOfSuccessfulInsert)
                R.id.update -> writeToDB(repository::updatePlant, ::notifyUserOfSuccessfulUpdate)
                else -> throw IllegalArgumentException("Unknown menuId: $menuId")
            }
        }
        return true
    }

    private fun isPlantNameEmpty() = plant.name.isEmpty()
    private fun isPlantDescriptionEmpty() = plant.description.isEmpty()
    private fun isPlantWateringPeriodEmpty() = plant.wateringPeriod.isEmpty()
    private fun isPlantImagePathEmpty() = plant.image.path.isEmpty()

    private fun shouldSavePlant(): Boolean {
        return !isPlantNameEmpty() && !isPlantDescriptionEmpty() &&
                !isPlantWateringPeriodEmpty() && !isPlantImagePathEmpty()
    }

    private fun notifyUserOfIncompletePlant() {
        val isPlantNameEmpty = isPlantNameEmpty()
        val isPlantDescriptionEmpty = isPlantDescriptionEmpty()
        val isPlantWateringPeriodEmpty = isPlantWateringPeriodEmpty()
        val isPlantImagePathEmpty = isPlantImagePathEmpty()

        when {
            isPlantNameEmpty && !isPlantDescriptionEmpty && !isPlantWateringPeriodEmpty &&
                    !isPlantImagePathEmpty -> notifyUserOfEmptyPlantName()
            isPlantNameEmpty && isPlantDescriptionEmpty && !isPlantWateringPeriodEmpty &&
                    !isPlantImagePathEmpty -> notifyUserOfEmptyPlantNameAndDescription()
            isPlantNameEmpty && !isPlantDescriptionEmpty && isPlantWateringPeriodEmpty &&
                    !isPlantImagePathEmpty -> notifyUserOfEmptyPlantNameAndWateringPeriod()
            isPlantNameEmpty && !isPlantDescriptionEmpty && !isPlantWateringPeriodEmpty &&
                    isPlantImagePathEmpty -> notifyUserOfEmptyPlantNameAndImagePath()
            isPlantNameEmpty && isPlantDescriptionEmpty && isPlantWateringPeriodEmpty &&
                    !isPlantImagePathEmpty -> notifyUserOfEmptyPlantNameDescriptionAndWateringPeriod()
            isPlantNameEmpty && isPlantDescriptionEmpty && !isPlantWateringPeriodEmpty &&
                    isPlantImagePathEmpty -> notifyUserOfEmptyPlantNameDescriptiondAndImagePath()
            isPlantNameEmpty && !isPlantDescriptionEmpty && isPlantWateringPeriodEmpty &&
                    isPlantImagePathEmpty -> notifyUserOfEmptyPlantNameWateringPeriodAndImagePath()
            !isPlantNameEmpty && isPlantDescriptionEmpty && !isPlantWateringPeriodEmpty &&
                    !isPlantImagePathEmpty -> notifyUserOfEmptyPlantDescription()
            !isPlantNameEmpty && isPlantDescriptionEmpty && isPlantWateringPeriodEmpty &&
                    !isPlantImagePathEmpty -> notifyUserOfEmptyPlantDescriptionAndWateringPeriod()
            !isPlantNameEmpty && isPlantDescriptionEmpty && !isPlantWateringPeriodEmpty &&
                    isPlantImagePathEmpty -> notifyUserOfEmptyPlantDescriptionAndImagePath()
            !isPlantNameEmpty && isPlantDescriptionEmpty && isPlantWateringPeriodEmpty &&
                    isPlantImagePathEmpty -> notifyUserOfEmptyPlantDescriptionWateringPeriodAndImagePath()
            !isPlantNameEmpty && !isPlantDescriptionEmpty && isPlantWateringPeriodEmpty &&
                    !isPlantImagePathEmpty -> notifyUserOfEmptyPlantWateringPeriod()
            !isPlantNameEmpty && !isPlantDescriptionEmpty && isPlantWateringPeriodEmpty &&
                    isPlantImagePathEmpty -> notifyUserOfEmptyPlantWateringPeriodAndImagePath()
            !isPlantNameEmpty && !isPlantDescriptionEmpty && !isPlantWateringPeriodEmpty &&
                    isPlantImagePathEmpty -> notifyUserOfEmptyPlantImagePath()
            isPlantNameEmpty && isPlantDescriptionEmpty && isPlantWateringPeriodEmpty &&
                    isPlantImagePathEmpty -> notifyUserOfCompleteEmptiness()
        }
    }

    private fun notifyUserOfEmptyPlantName() {
        _showSnackBar.tryEmit(R.string.plant_name_empty)
    }

    private fun notifyUserOfEmptyPlantNameAndDescription() {
        _showSnackBar.tryEmit(R.string.plant_name_and_description_empty)
    }

    private fun notifyUserOfEmptyPlantNameAndWateringPeriod() {
        _showSnackBar.tryEmit(R.string.plant_name_and_watering_period_empty)
    }

    private fun notifyUserOfEmptyPlantNameAndImagePath() {
        _showSnackBar.tryEmit(R.string.plant_name_and_image_path_empty)
    }

    private fun notifyUserOfEmptyPlantNameDescriptionAndWateringPeriod() {
        _showSnackBar.tryEmit(R.string.plant_name_description_and_watering_period_empty)
    }

    private fun notifyUserOfEmptyPlantNameDescriptiondAndImagePath() {
        _showSnackBar.tryEmit(R.string.plant_name_description_and_image_path_empty)
    }

    private fun notifyUserOfEmptyPlantNameWateringPeriodAndImagePath() {
        _showSnackBar.tryEmit(R.string.plant_name_watering_period_and_image_path_empty)
    }

    private fun notifyUserOfEmptyPlantDescription() {
        _showSnackBar.tryEmit(R.string.plant_description_empty)
    }

    private fun notifyUserOfEmptyPlantDescriptionAndWateringPeriod() {
        _showSnackBar.tryEmit(R.string.plant_description_and_watering_period_empty)
    }

    private fun notifyUserOfEmptyPlantDescriptionAndImagePath() {
        _showSnackBar.tryEmit(R.string.plant_description_and_image_path_empty)
    }

    private fun notifyUserOfEmptyPlantDescriptionWateringPeriodAndImagePath() {
        _showSnackBar.tryEmit(R.string.plant_description_watering_period_and_image_path_empty)
    }

    private fun notifyUserOfEmptyPlantWateringPeriod() {
        _showSnackBar.tryEmit(R.string.plant_watering_period_empty)
    }

    private fun notifyUserOfEmptyPlantWateringPeriodAndImagePath() {
        _showSnackBar.tryEmit(R.string.plant_watering_period_and_image_path_empty)
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

    private fun notifyUserOfSuccessfulUpdate() {
        _showSnackBar.tryEmit(R.string.plant_successfully_updated)
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

    fun confirmReverse() {
        _confirmReverseEvent.tryEmit(true)
    }

    fun onYesClicked() {
        _confirmReverseEvent.tryEmit(false)
        reverseChanges()
        _showSnackBar.tryEmit(R.string.dialog_reversed)
    }

    fun onNoClicked() {
        _confirmReverseEvent.tryEmit(false)
    }

    private fun reverseChanges() {
        plant.name = unmodifiedPlant.name
        plant.description = unmodifiedPlant.description
        plant.wateringPeriod = unmodifiedPlant.wateringPeriod
        plant.image = Image(unmodifiedPlant.image.path, unmodifiedPlant.image.rotationAngle)
        _plantState.value = PlantState.VIEWING to PlantState.EDITING
    }
    //endregion
}