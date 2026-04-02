package com.gcancino.levelingup.core.di

import com.gcancino.levelingup.data.repositories.*
import com.gcancino.levelingup.domain.repositories.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    abstract fun bindPlayerRepository(
        playerRepositoryImpl: PlayerRepositoryImpl
    ): PlayerRepository


    @Binds
    abstract fun bindQuestRepository(
        questRepositoryImpl: QuestRepositoryImpl
    ): QuestRepository

    @Binds
    abstract fun bindExerciseRepository(
        exerciseRepositoryImpl: ExerciseRepositoryImpl
    ): ExerciseRepository

    @Binds
    @Singleton
    abstract fun bindBodyDataRepository(impl: BodyDataRepositoryImpl): BodyDataRepository

    @Binds
    @Singleton
    abstract fun bindDailyTasksRepository(impl: DailyTasksRepositoryImpl): DailyTasksRepository
}
