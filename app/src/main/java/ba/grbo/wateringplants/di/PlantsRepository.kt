package ba.grbo.wateringplants.di

import ba.grbo.wateringplants.data.source.DefaultPlantsRepository
import ba.grbo.wateringplants.data.source.PlantsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class PlantsRepository {

    @Singleton
    @Binds
    abstract fun bindPlantsRepository(implementation: DefaultPlantsRepository): PlantsRepository
}