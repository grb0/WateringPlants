package ba.grbo.wateringplants.data.source

import ba.grbo.wateringplants.data.DatabasePlant
import ba.grbo.wateringplants.data.Result
import kotlinx.coroutines.flow.Flow

interface PlantsRepository {
    suspend fun insertPlant(plant: DatabasePlant): Result<Unit>

    fun getAllPlants(): Flow<Result<List<DatabasePlant>>>
}