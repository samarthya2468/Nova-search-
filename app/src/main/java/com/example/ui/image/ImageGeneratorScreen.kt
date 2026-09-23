package com.example.ui.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.GeneratedImageEntity
import com.example.data.remote.GeneratedImageResult
import com.example.data.repository.ImageRepository
import com.example.ui.components.ImageSkeletonScreen
import com.example.ui.components.PromptHistoryBottomSheet
import com.example.ui.components.copyToClipboard
import com.example.ui.components.shareText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageGeneratorScreen(
    repository: ImageRepository,
    initialPrompt: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var promptText by remember { mutableStateOf(initialPrompt ?: "") }
    var selectedStyle by remember { mutableStateOf("Photorealistic") }
    var selectedAspectRatio by remember { mutableStateOf("1:1") }

    var isGenerating by remember { mutableStateOf(false) }
    var currentResult by remember { mutableStateOf<GeneratedImageResult?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var fullscreenBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showPromptHistorySheet by remember { mutableStateOf(false) }

    val recentImages by repository.imageHistory.collectAsState(initial = emptyList())

    val styles = listOf(
        "Photorealistic" to "📸 Photo",
        "Cyberpunk" to "🌌 Cyberpunk",
        "Anime" to "🎨 Anime",
        "3D Render" to "🧊 3D Render",
        "Oil Painting" to "🖌️ Oil Painting",
        "Fantasy Art" to "✨ Fantasy Art",
        "Minimalist Vector" to "📐 Minimalist"
    )

    val aspectRatios = listOf(
        "1:1" to "1:1 Square",
        "16:9" to "16:9 Cinema",
        "9:16" to "9:16 Story",
        "4:3" to "4:3 Standard"
    )

    val promptSparks = listOf(
        "Futuristic neon coffee shop in rainy Neo-Tokyo, holographic menu, atmospheric lighting",
        "Majestic crystalline phoenix rising over an ancient aurora mountain",
        "Isometric 3D miniature cozy library with tiny floating magical books and plants",
        "Adorable robotic kitten playing with a golden glowing butterfly in a cyber garden",
        "Minimalist geometric illustration of a solar eclipse over a quiet desert"
    )

    fun startGeneration(promptToUse: String) {
        if (promptToUse.isBlank()) return
        promptText = promptToUse
        keyboardController?.hide()
        isGenerating = true
        coroutineScope.launch {
            val result = repository.generateImage(
                prompt = promptToUse,
                style = selectedStyle,
                aspectRatio = selectedAspectRatio
            )
            currentResult = result
            previewBitmap = result.bitmap
            isGenerating = false
        }
    }

    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank() && currentResult == null) {
            promptText = initialPrompt
        }
    }

    // Fullscreen Image Dialog
    fullscreenBitmap?.let { bmp ->
        Dialog(onDismissRequest = { fullscreenBitmap = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Fullscreen image preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { fullscreenBitmap = null }) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Generator Controls Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("image_prompt_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "AI Image Studio",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        AssistChip(
                            onClick = {
                                if (promptText.isNotBlank()) {
                                    promptText = "$promptText, ultra detailed, cinematic volumetric lighting, 8k resolution"
                                }
                            },
                            label = { Text("✨ Enhance", fontSize = 11.sp) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }

                    // Prompt Input Field
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("image_prompt_input"),
                        placeholder = {
                            Text(
                                "Describe the image you want to generate in detail...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        trailingIcon = {
                            if (promptText.isNotBlank()) {
                                IconButton(onClick = { promptText = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Style Selection Chips
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Artistic Style",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            styles.forEach { (styleKey, styleLabel) ->
                                val isSelected = selectedStyle == styleKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedStyle = styleKey },
                                    label = { Text(styleLabel, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Aspect Ratio Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Aspect Ratio",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            aspectRatios.forEach { (ratioKey, ratioLabel) ->
                                val isSelected = selectedAspectRatio == ratioKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedAspectRatio = ratioKey },
                                    label = { Text(ratioLabel, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }

                    // Generate Button
                    Button(
                        onClick = { startGeneration(promptText) },
                        enabled = promptText.isNotBlank() && !isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("generate_image_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Generating Artwork...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Image", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Quick Prompt History Bar (Always visible when generated art exists in Room DB)
        if (recentImages.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Prompt History",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            TextButton(
                                onClick = { showPromptHistorySheet = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("All (${recentImages.size}) →", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recentImages.take(8).forEach { item ->
                                SuggestionChip(
                                    onClick = {
                                        promptText = item.prompt
                                        selectedStyle = item.style
                                        selectedAspectRatio = item.aspectRatio
                                    },
                                    label = { Text(item.prompt, maxLines = 1, fontSize = 12.sp) },
                                    icon = {
                                        Icon(
                                            imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.Brush,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (item.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                                        )
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Skeleton Screen & Indeterminate Loading State for Art Generation
        if (isGenerating) {
            item {
                ImageSkeletonScreen(
                    prompt = promptText,
                    style = selectedStyle,
                    aspectRatio = selectedAspectRatio
                )
            }
        }

        // Generated Artwork Result
        if (!isGenerating) {
            previewBitmap?.let { bitmap ->
                item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generated_image_result_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "$selectedStyle • $selectedAspectRatio",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = { fullscreenBitmap = bitmap }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = "Fullscreen",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        copyToClipboard(context, "Image Prompt", promptText)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = "Copy prompt",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        shareText(context, "NovaSearch AI Art", "Created with NovaSearch AI: \"$promptText\"")
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Share,
                                        contentDescription = "Share",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Image Preview Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { fullscreenBitmap = bitmap }
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Generated art for $promptText",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(
                                        when (selectedAspectRatio) {
                                            "16:9" -> 16f / 9f
                                            "9:16" -> 9f / 16f
                                            "4:3" -> 4f / 3f
                                            else -> 1f
                                        }
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Text(
                            text = "\"$promptText\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        currentResult?.errorMessage?.let { errorMsg ->
                            Text(
                                text = "Notice: $errorMsg",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }

        // Inspiration Prompts (Prompt Sparks)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Prompt Sparks & Ideas",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    promptSparks.forEach { spark ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    promptText = spark
                                    startGeneration(spark)
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Brush,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = spark,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Generate this",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Generated Images Gallery
        if (recentImages.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Created Gallery",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        TextButton(onClick = { coroutineScope.launch { repository.clearHistory() } }) {
                            Text("Clear", fontSize = 12.sp)
                        }
                    }

                    // 2-column image grid representation
                    val chunked = recentImages.chunked(2)
                    chunked.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { item ->
                                val bmp = remember(item.imageBase64) {
                                    try {
                                        val bytes = Base64.decode(item.imageBase64, Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            promptText = item.prompt
                                            selectedStyle = item.style
                                            selectedAspectRatio = item.aspectRatio
                                            bmp?.let { fullscreenBitmap = it }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column {
                                        if (bmp != null) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = item.prompt,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(110.dp),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = item.prompt,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = item.style,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPromptHistorySheet) {
        PromptHistoryBottomSheet(
            repository = repository,
            onDismiss = { showPromptHistorySheet = false },
            onSelectPrompt = { item ->
                promptText = item.prompt
                selectedStyle = item.style
                selectedAspectRatio = item.aspectRatio
            }
        )
    }
}
