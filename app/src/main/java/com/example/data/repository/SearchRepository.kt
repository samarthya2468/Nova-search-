package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.SearchHistoryEntity
import com.example.data.remote.GeminiClient
import com.example.data.remote.SearchResult
import com.example.ui.chat.AttachedFile
import kotlinx.coroutines.flow.Flow

class SearchRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val searchDao = database.searchDao()

    val searchHistory: Flow<List<SearchHistoryEntity>> = searchDao.getAllSearches()
    val favoriteSearches: Flow<List<SearchHistoryEntity>> = searchDao.getFavoriteSearches()

    suspend fun executeSearch(
        query: String,
        mode: String = "Quick",
        customPrompt: String? = null,
        attachments: List<AttachedFile> = emptyList()
    ): SearchResult {
        val result = GeminiClient.search(context, query, mode, customPrompt, attachments)

        // Save to Room database if search produced a valid answer
        if (result.answer.isNotBlank()) {
            val displayQuery = if (attachments.isNotEmpty()) {
                val fileNames = attachments.joinToString(", ") { it.name }
                if (query.isNotBlank()) "📎 [$fileNames] $query" else "📎 [$fileNames]"
            } else {
                query
            }

            val entity = SearchHistoryEntity(
                query = displayQuery,
                answer = result.answer,
                mode = mode,
                customPrompt = customPrompt,
                keyTakeaways = result.keyTakeaways.joinToString("||"),
                followUpQuestions = result.followUpQuestions.joinToString("||"),
                timestamp = System.currentTimeMillis()
            )
            searchDao.insertSearch(entity)
        }

        return result
    }

    suspend fun toggleFavorite(search: SearchHistoryEntity) {
        searchDao.updateSearch(search.copy(isFavorite = !search.isFavorite))
    }

    suspend fun deleteSearch(id: Long) {
        searchDao.deleteSearchById(id)
    }

    suspend fun clearHistory() {
        searchDao.clearAllSearches()
    }

    fun searchHistoryWithQuery(keyword: String): Flow<List<SearchHistoryEntity>> {
        return searchDao.searchHistory(keyword)
    }
}
