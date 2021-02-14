package ba.grbo.wateringplants.data.source

import ba.grbo.wateringplants.data.Plant
import ba.grbo.wateringplants.data.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DefaultPlantsRepository @Inject constructor(
    private val plantsSource: PlantsSource
) : PlantsRepository {
    override suspend fun insertPlant(plant: Plant): Result<Boolean> =
        plantsSource.insertPlant(plant)

    override suspend fun updatePlant(plant: Plant): Result<Boolean> =
        plantsSource.updatePlant(plant)

    override suspend fun getPlant(plantId: Int): Result<Plant> = plantsSource.getPlant(plantId)

    override fun getAllPlants(): Flow<Result<List<Plant>>> = plantsSource.getAllPlants()
}