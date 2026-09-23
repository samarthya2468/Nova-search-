package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchDao {
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    fun getAllSearches(): Flow<List<SearchHistoryEntity>>

    @Query("SELECT * FROM search_history WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteSearches(): Flow<List<SearchHistoryEntity>>

    @Query("SELECT * FROM search_history WHERE query LIKE '%' || :keyword || '%' OR answer LIKE '%' || :keyword || '%' ORDER BY timestamp DESC")
    fun searchHistory(keyword: String): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: SearchHistoryEntity): Long

    @Update
    suspend fun updateSearch(search: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteSearchById(id: Long)

    @Query("DELETE FROM search_history")
    suspend fun clearAllSearches()
}

@Dao
interface ImageDao {
    @Query("SELECT * FROM generated_images ORDER BY timestamp DESC")
    fun getAllImages(): Flow<List<GeneratedImageEntity>>

    @Query("SELECT * FROM generated_images WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteImages(): Flow<List<GeneratedImageEntity>>

    @Query("SELECT * FROM generated_images WHERE prompt LIKE '%' || :keyword || '%' ORDER BY timestamp DESC")
    fun searchImages(keyword: String): Flow<List<GeneratedImageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: GeneratedImageEntity): Long

    @Update
    suspend fun updateImage(image: GeneratedImageEntity)

    @Query("DELETE FROM generated_images WHERE id = :id")
    suspend fun deleteImageById(id: Long)

    @Query("DELETE FROM generated_images")
    suspend fun clearAllImages()
}
