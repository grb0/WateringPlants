package ba.grbo.wateringplants.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import ba.grbo.wateringplants.data.Plant
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {
    @Insert
    suspend fun insert(plant: Plant)

    @Query("SELECT * FROM plants_table ORDER BY id DESC")
    fun plantsStream(): Flow<List<Plant>>
}