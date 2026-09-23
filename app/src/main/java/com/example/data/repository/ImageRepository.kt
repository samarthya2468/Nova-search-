package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.GeneratedImageEntity
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeneratedImageResult
import kotlinx.coroutines.flow.Flow

class ImageRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val imageDao = database.imageDao()

    val imageHistory: Flow<List<GeneratedImageEntity>> = imageDao.getAllImages()
    val favoriteImages: Flow<List<GeneratedImageEntity>> = imageDao.getFavoriteImages()

    suspend fun generateImage(
        prompt: String,
        style: String,
        aspectRatio: String
    ): GeneratedImageResult {
        val result = GeminiClient.generateImage(context, prompt, style, aspectRatio)

        if (result.imageBase64 != null) {
            val entity = GeneratedImageEntity(
                prompt = prompt,
                style = style,
                aspectRatio = aspectRatio,
                imageBase64 = result.imageBase64,
                mimeType = result.mimeType,
                timestamp = System.currentTimeMillis()
            )
            imageDao.insertImage(entity)
        }

        return result
    }

    suspend fun toggleFavorite(image: GeneratedImageEntity) {
        imageDao.updateImage(image.copy(isFavorite = !image.isFavorite))
    }

    suspend fun deleteImage(id: Long) {
        imageDao.deleteImageById(id)
    }

    suspend fun clearHistory() {
        imageDao.clearAllImages()
    }
}
