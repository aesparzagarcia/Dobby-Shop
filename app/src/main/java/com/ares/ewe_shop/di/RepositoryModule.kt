package com.ares.ewe_shop.di

import com.ares.ewe_shop.data.repository.AuthRepositoryImpl
import com.ares.ewe_shop.data.repository.OrderRepositoryImpl
import com.ares.ewe_shop.domain.repository.AuthRepository
import com.ares.ewe_shop.domain.repository.OrderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository
}
