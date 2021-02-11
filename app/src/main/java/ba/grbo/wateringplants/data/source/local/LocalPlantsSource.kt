package ba.grbo.wateringplants.data.source.local

import ba.grbo.wateringplants.data.Plant
import ba.grbo.wateringplants.data.Result
import ba.grbo.wateringplants.data.Result.Error
import ba.grbo.wateringplants.data.Result.Success
import ba.grbo.wateringplants.data.source.PlantsSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalPlantsSource @Inject constructor(
    private val plantDao: PlantDao,
) : PlantsSource {
    override suspend fun insertPlant(plant: Plant): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            plantDao.insert(plant)
            Success(Unit)
        } catch (e: Exception) {
            Error(e)
        }
    }

    override fun getAllPlants(): Flow<Result<List<Plant>>> = try {
        plantDao.plantsStream().map { Success(it) }.flowOn(Dispatchers.Default)
    } catch (e: Exception) {
        flow { emit(Error(e)) }
    }
}