package com.semenov.news.core.db.di

import android.content.Context
import androidx.room.Room
import com.semenov.news.core.db.NewsDatabase
import com.semenov.news.core.db.dao.ArticleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideNewsDatabase(
        @ApplicationContext context: Context,
    ): NewsDatabase =
        Room.databaseBuilder(
            context,
            NewsDatabase::class.java,
            DATABASE_NAME,
        ).fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideArticleDao(database: NewsDatabase): ArticleDao = database.articleDao()

    private const val DATABASE_NAME = "news.db"
}
