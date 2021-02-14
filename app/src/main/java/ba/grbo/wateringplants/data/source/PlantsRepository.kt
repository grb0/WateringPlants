package ba.grbo.wateringplants.data.source

import ba.grbo.wateringplants.data.Plant
import ba.grbo.wateringplants.data.Result
import kotlinx.coroutines.flow.Flow

interface PlantsRepository {
    suspend fun insertPlant(plant: Plant): Result<Boolean>

    suspend fun updatePlant(plant: Plant): Result<Boolean>

    suspend fun getPlant(plantId: Int): Result<Plant>

    fun getAllPlants(): Flow<Result<List<Plant>>>
}