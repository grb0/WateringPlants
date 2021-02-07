package ba.grbo.wateringplants.di

import android.content.Context
import ba.grbo.wateringplants.data.source.local.PlantsDB
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object PlantsDBModule {
    @Provides
    fun providePlantDao(db: PlantsDB) = db.plantDao

    @Singleton
    @Provides
    fun providePlantsDB(@ApplicationContext appContext: Context) = PlantsDB.getInstance(appContext)
}