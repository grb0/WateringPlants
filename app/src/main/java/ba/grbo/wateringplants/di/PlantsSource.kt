package ba.grbo.wateringplants.di

import ba.grbo.wateringplants.data.source.PlantsSource
import ba.grbo.wateringplants.data.source.local.LocalPlantsSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class PlantsSource {

    @Singleton
    @Binds
    abstract fun bindPlantsSource(implementation: LocalPlantsSource): PlantsSource
}