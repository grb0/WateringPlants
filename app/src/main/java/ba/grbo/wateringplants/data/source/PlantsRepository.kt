package ba.grbo.wateringplants.data.source

import ba.grbo.wateringplants.data.Plant
import ba.grbo.wateringplants.data.Result
import kotlinx.coroutines.flow.Flow

interface PlantsRepository {
    suspend fun insertPlant(plant: Plant): Result<Unit>

    fun getAllPlants(): Flow<Result<List<Plant>>>
}