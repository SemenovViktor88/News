package com.semenov.news.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.semenov.news.core.db.dao.ArticleDao
import com.semenov.news.core.db.entity.ArticleEntity

@Database(
    entities = [ArticleEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class NewsDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
}
