package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "generated_images")
data class GeneratedImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val prompt: String,
    val style: String = "Photorealistic",
    val aspectRatio: String = "1:1",
    val imageBase64: String,
    val mimeType: String = "image/jpeg",
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
