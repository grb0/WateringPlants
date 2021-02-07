package ba.grbo.wateringplants.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

data class Plant(
    val name: MutableStateFlow<String>,
    val description: Flow<String>,
    val wateringPeriod: Flow<String>,
    val imagePath: Flow<String>? = null
)