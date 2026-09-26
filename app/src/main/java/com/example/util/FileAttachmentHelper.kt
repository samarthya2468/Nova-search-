package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.example.ui.chat.AttachedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Locale

object FileAttachmentHelper {

    suspend fun processUri(context: Context, uri: Uri): AttachedFile? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver

            // 1. Get file name & size from ContentResolver
            var displayName = "file"
            var sizeBytes = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex) ?: displayName
                    }
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }

            // 2. Resolve MIME type
            var mimeType = contentResolver.getType(uri) ?: getMimeTypeFromName(displayName)

            // 3. Read stream bytes safely (limit to 12MB)
            val maxBytes = 12 * 1024 * 1024
            val byteArray = contentResolver.openInputStream(uri)?.use { stream ->
                readBytesWithLimit(stream, maxBytes)
            } ?: return@withContext null

            if (sizeBytes == 0L) {
                sizeBytes = byteArray.size.toLong()
            }

            var base64Data: String? = null
            var textContent: String? = null
            var previewBitmap: Bitmap? = null

            // 4. Handle based on content type
            if (mimeType.startsWith("image/")) {
                // Decode preview bitmap with sample size to preserve memory
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)

                // Scale down if larger than 1200px
                var sampleSize = 1
                while (options.outWidth / sampleSize > 1200 || options.outHeight / sampleSize > 1200) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
                previewBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, decodeOptions)

                // Compress to JPEG for API transport
                val outputStream = ByteArrayOutputStream()
                previewBitmap?.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                val compressedBytes = outputStream.toByteArray()
                base64Data = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
                mimeType = "image/jpeg"
            } else if (mimeType == "application/pdf" || displayName.lowercase(Locale.ROOT).endsWith(".pdf")) {
                mimeType = "application/pdf"
                base64Data = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            } else if (isTextFile(displayName, mimeType)) {
                // Text/code/data file: decode string
                textContent = try {
                    String(byteArray, Charsets.UTF_8).take(150_000)
                } catch (e: Exception) {
                    String(byteArray).take(150_000)
                }
                base64Data = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            } else {
                // Other file types (DOC, audio, binary)
                base64Data = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            }

            AttachedFile(
                name = displayName,
                mimeType = mimeType,
                sizeBytes = sizeBytes,
                base64Data = base64Data,
                textContent = textContent,
                previewBitmap = previewBitmap
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun readBytesWithLimit(inputStream: InputStream, limit: Int): ByteArray {
        val buffer = ByteArrayOutputStream()
        val data = ByteArray(8192)
        var totalRead = 0
        var nRead: Int
        while (inputStream.read(data, 0, data.size).also { nRead = it } != -1) {
            if (totalRead + nRead > limit) {
                buffer.write(data, 0, limit - totalRead)
                break
            }
            buffer.write(data, 0, nRead)
            totalRead += nRead
        }
        return buffer.toByteArray()
    }

    private fun isTextFile(name: String, mime: String): Boolean {
        val lower = name.lowercase(Locale.ROOT)
        return mime.startsWith("text/") ||
                mime.contains("json") ||
                mime.contains("xml") ||
                mime.contains("javascript") ||
                mime.contains("csv") ||
                lower.endsWith(".txt") ||
                lower.endsWith(".csv") ||
                lower.endsWith(".json") ||
                lower.endsWith(".md") ||
                lower.endsWith(".py") ||
                lower.endsWith(".kt") ||
                lower.endsWith(".java") ||
                lower.endsWith(".c") ||
                lower.endsWith(".cpp") ||
                lower.endsWith(".js") ||
                lower.endsWith(".ts") ||
                lower.endsWith(".html") ||
                lower.endsWith(".css") ||
                lower.endsWith(".xml") ||
                lower.endsWith(".log") ||
                lower.endsWith(".sh") ||
                lower.endsWith(".sql")
    }

    private fun getMimeTypeFromName(name: String): String {
        val lower = name.lowercase(Locale.ROOT)
        return when {
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".gif") -> "image/gif"
            lower.endsWith(".txt") -> "text/plain"
            lower.endsWith(".csv") -> "text/csv"
            lower.endsWith(".json") -> "application/json"
            lower.endsWith(".md") -> "text/markdown"
            lower.endsWith(".py") -> "text/x-python"
            lower.endsWith(".kt") -> "text/x-kotlin"
            lower.endsWith(".java") -> "text/x-java-source"
            lower.endsWith(".html") -> "text/html"
            lower.endsWith(".xml") -> "application/xml"
            lower.endsWith(".mp3") -> "audio/mp3"
            lower.endsWith(".wav") -> "audio/wav"
            else -> "application/octet-stream"
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format(Locale.US, "%.1f MB", bytes.toDouble() / (1024 * 1024))
        }
    }
}
