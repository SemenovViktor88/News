package com.semenov.news.features.auth.data.session

import com.semenov.news.core.db.NewsDatabase
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface SessionDataCleaner {
    suspend fun clearDatabase()
}

class DatabaseSessionDataCleaner @Inject constructor(
    private val database: NewsDatabase,
) : SessionDataCleaner {
    override suspend fun clearDatabase() = withContext(Dispatchers.IO) {
        database.clearAllTables()
    }
}
