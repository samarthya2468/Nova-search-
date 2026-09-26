package com.example.ui.chat

import android.graphics.Bitmap
import java.util.UUID

data class AttachedFile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val base64Data: String? = null,
    val textContent: String? = null,
    val previewBitmap: Bitmap? = null
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val mode: String = "Quick",
    val attachedFiles: List<AttachedFile> = emptyList(),
    val imageBitmap: Bitmap? = null,
    val imageBase64: String? = null,
    val style: String? = null,
    val aspectRatio: String? = null,
    val keyTakeaways: List<String> = emptyList(),
    val followUpQuestions: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
