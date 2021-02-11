package ba.grbo.wateringplants.data.source

import ba.grbo.wateringplants.data.Plant
import ba.grbo.wateringplants.data.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DefaultPlantsRepository @Inject constructor(
    private val plantsSource: PlantsSource
) : PlantsRepository {
    override suspend fun insertPlant(plant: Plant): Result<Unit> = plantsSource.insertPlant(plant)

    override fun getAllPlants(): Flow<Result<List<Plant>>> = plantsSource.getAllPlants()
}