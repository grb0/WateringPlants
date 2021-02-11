package ba.grbo.wateringplants.data.source.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ba.grbo.wateringplants.data.Plant

@Database(entities = [Plant::class], version = 1)
@TypeConverters(Converter::class)
abstract class PlantsDB : RoomDatabase() {
    abstract val plantDao: PlantDao

    companion object {
        @Volatile
        private lateinit var INSTANCE: PlantsDB

        fun getInstance(appContext: Context) = if (::INSTANCE.isInitialized) INSTANCE
        else synchronized(this) { buildDB(appContext).also { INSTANCE = it } }

        private fun buildDB(appContext: Context) = Room.databaseBuilder(
            appContext,
            PlantsDB::class.java,
            "plants_db"
        ).fallbackToDestructiveMigration().build()
    }
}