package ba.grbo.wateringplants.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import ba.grbo.wateringplants.data.Plant
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {
    @Insert
    suspend fun insert(plant: Plant)

    @Update
    suspend fun update(plant: Plant)

    @Query("SELECT * FROM plants_table WHERE id = :id")
    suspend fun get(id: Int): Plant

    @Query("SELECT * FROM plants_table ORDER BY id DESC")
    fun plantsStream(): Flow<List<Plant>>
}