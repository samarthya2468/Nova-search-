package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val query: String,
    val answer: String,
    val mode: String = "Quick", // "Quick", "Deep Dive", "Step-by-Step", "Code", "Custom"
    val customPrompt: String? = null,
    val keyTakeaways: String = "", // Comma/newline separated takeaways
    val followUpQuestions: String = "", // Comma/newline separated follow ups
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
