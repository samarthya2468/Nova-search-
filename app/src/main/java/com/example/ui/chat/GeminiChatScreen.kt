package com.example.ui.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.SearchHistoryEntity
import com.example.data.remote.GeminiClient
import com.example.data.repository.ImageRepository
import com.example.data.repository.SearchRepository
import com.example.ui.components.ImageSkeletonScreen
import com.example.ui.components.RichAnswerRenderer
import com.example.ui.components.SearchSkeletonScreen
import com.example.ui.components.copyToClipboard
import com.example.ui.components.shareText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiChatScreen(
    searchRepository: SearchRepository,
    imageRepository: ImageRepository,
    activeChatEntity: SearchHistoryEntity?,
    onOpenDrawer: () -> Unit,
    onNewChat: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("Quick") }
    var isImageMode by remember { mutableStateOf(false) }
    var customPromptModifier by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var showToolMenu by remember { mutableStateOf(false) }
    var fullscreenBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Chat messages for current session
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }

    // When an active chat is selected from the drawer, load it into messages
    LaunchedEffect(activeChatEntity?.id) {
        if (activeChatEntity != null) {
            val takeaways = if (activeChatEntity.keyTakeaways.isNotBlank()) {
                activeChatEntity.keyTakeaways.split("||").filter { it.isNotBlank() }
            } else emptyList()

            val followUps = if (activeChatEntity.followUpQuestions.isNotBlank()) {
                activeChatEntity.followUpQuestions.split("||").filter { it.isNotBlank() }
            } else emptyList()

            messages = listOf(
                ChatMessage(
                    isUser = true,
                    text = activeChatEntity.query,
                    timestamp = activeChatEntity.timestamp
                ),
                ChatMessage(
                    isUser = false,
                    text = activeChatEntity.answer,
                    mode = activeChatEntity.mode,
                    keyTakeaways = takeaways,
                    followUpQuestions = followUps,
                    timestamp = activeChatEntity.timestamp
                )
            )
            selectedMode = activeChatEntity.mode
            isImageMode = activeChatEntity.mode == "Image"
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun handleSend(prompt: String) {
        if (prompt.isBlank() || isLoading) return
        val currentPrompt = prompt.trim()
        inputText = ""
        keyboardController?.hide()

        val userMessage = ChatMessage(
            isUser = true,
            text = currentPrompt,
            mode = if (isImageMode) "Image" else selectedMode
        )
        messages = messages + userMessage
        isLoading = true

        coroutineScope.launch {
            if (isImageMode) {
                // Generate Image via ImageRepository
                val result = imageRepository.generateImage(
                    prompt = currentPrompt,
                    style = "Photorealistic",
                    aspectRatio = "1:1"
                )

                val bmp = result.bitmap ?: result.imageBase64?.let { base64 ->
                    try {
                        val bytes = Base64.decode(base64, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (e: Exception) {
                        null
                    }
                }

                val aiMessage = ChatMessage(
                    isUser = false,
                    text = if (bmp != null) "Here is the visual artwork generated for: \"$currentPrompt\"" else (result.errorMessage ?: "Failed to generate image"),
                    mode = "Image",
                    imageBitmap = bmp,
                    imageBase64 = result.imageBase64,
                    style = "Photorealistic",
                    aspectRatio = "1:1"
                )
                messages = messages + aiMessage
                isLoading = false
            } else {
                // Execute Search / Conversation via SearchRepository
                val result = searchRepository.executeSearch(
                    query = currentPrompt,
                    mode = selectedMode,
                    customPrompt = customPromptModifier.ifBlank { null }
                )

                val aiMessage = ChatMessage(
                    isUser = false,
                    text = result.answer,
                    mode = selectedMode,
                    keyTakeaways = result.keyTakeaways,
                    followUpQuestions = result.followUpQuestions
                )
                messages = messages + aiMessage
                isLoading = false
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("gemini_chat_scaffold"),
        topBar = {
            // Sleek Gemini Top Bar matching Screenshot_20260923_200146.jpg
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hamburger menu button to open taskbar drawer
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .testTag("gemini_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open taskbar drawer",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Center Model Badge Pill
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isImageMode) "NovaStudio • Image" else "NovaSearch • $selectedMode",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Right: Settings key icon + New Chat circular icon button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .testTag("gemini_top_settings_button")
                    ) {
                        Icon(
                            imageVector = if (GeminiClient.isApiKeyConfigured(context)) Icons.Outlined.Settings else Icons.Default.Key,
                            contentDescription = "AI Settings",
                            tint = if (GeminiClient.isApiKeyConfigured(context)) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            messages = emptyList()
                            isImageMode = false
                            selectedMode = "Quick"
                            onNewChat()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .testTag("gemini_top_new_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = "New Chat",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            // THE SEARCH / PROMPT BAR DOCKED AT THE BOTTOM (smoothly rises with IME keyboard)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("gemini_bottom_input_container")
            ) {
                // Contextual Quick Chips directly above the bottom input bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Create an Image toggle pill (matching screenshot)
                    FilterChip(
                        selected = isImageMode,
                        onClick = { isImageMode = !isImageMode },
                        label = { Text("Create an image", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Image,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )

                    // Think harder / Deep dive pill (matching screenshot)
                    FilterChip(
                        selected = selectedMode == "Deep Dive" && !isImageMode,
                        onClick = {
                            isImageMode = false
                            selectedMode = if (selectedMode == "Deep Dive") "Quick" else "Deep Dive"
                        },
                        label = { Text("Think harder", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(16.dp)
                    )

                    FilterChip(
                        selected = selectedMode == "Code & Logic" && !isImageMode,
                        onClick = {
                            isImageMode = false
                            selectedMode = if (selectedMode == "Code & Logic") "Quick" else "Code & Logic"
                        },
                        label = { Text("Code & Logic", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Code,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                // Sleek Pill Input Bar matching Gemini's bottom search bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left "+" tool button (opens popover menu like Screenshot_20260923_200146.jpg)
                        IconButton(
                            onClick = { showToolMenu = !showToolMenu },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .testTag("gemini_tool_menu_button")
                        ) {
                            Icon(
                                imageVector = if (showToolMenu) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = "Add tools & modes",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Text Input Field
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    text = if (isImageMode) "Describe an image to generate..." else "Ask anything...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("gemini_chat_input"),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { handleSend(inputText) }),
                            maxLines = 4,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        // Right Action: Send button or Voice/Soundwave button
                        if (inputText.isNotBlank()) {
                            FilledIconButton(
                                onClick = { handleSend(inputText) },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("gemini_send_button"),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (isImageMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send prompt",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { /* Mic input */ },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Voice input",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = "Live",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (messages.isEmpty() && !isLoading) {
                // Clean Minimal Empty State (NO decorative stock images as requested!)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "Hello! How can I help you today?",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "Ask questions, explore ideas, write code, or create AI visual artwork.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Starter Prompt Cards
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val starters = listOf(
                                "Create an image of a cybernetic tiger in a neon rainforest" to true,
                                "Explain quantum computing using a simple everyday analogy" to false,
                                "Summarize the key architectural benefits of Kotlin Coroutines & Flow" to false,
                                "Write a clean Kotlin function to debounce user input in Compose" to false
                            )

                            starters.forEach { (prompt, isImg) ->
                                Surface(
                                    onClick = {
                                        isImageMode = isImg
                                        handleSend(prompt)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = CardDefaults.outlinedCardBorder(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isImg) Icons.Outlined.Palette else Icons.Outlined.Lightbulb,
                                            contentDescription = null,
                                            tint = if (isImg) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = prompt,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Conversational Message List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        if (message.isUser) {
                            // User Message Bubble (Right-aligned, sleek)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = CardDefaults.outlinedCardBorder(),
                                    modifier = Modifier.widthIn(max = 320.dp)
                                ) {
                                    Text(
                                        text = message.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                            }
                        } else {
                            // AI Message Bubble (Left-aligned, Sparkle badge, Rich formatted text)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (message.mode == "Image") MaterialTheme.colorScheme.secondary
                                                else MaterialTheme.colorScheme.primary
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (message.mode == "Image") Icons.Default.Palette else Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    Text(
                                        text = if (message.mode == "Image") "NovaStudio Image" else "NovaSearch • ${message.mode}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = CardDefaults.outlinedCardBorder(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // If AI generated an image: render image in message!
                                        if (message.imageBitmap != null) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = 300.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable { fullscreenBitmap = message.imageBitmap },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Image(
                                                    bitmap = message.imageBitmap.asImageBitmap(),
                                                    contentDescription = "AI Generated Artwork",
                                                    modifier = Modifier.fillMaxWidth(),
                                                    contentScale = ContentScale.Fit
                                                )
                                            }

                                            Text(
                                                text = message.text,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            // Rich Markdown & Code formatting
                                            RichAnswerRenderer(text = message.text)
                                        }

                                        // Key Takeaways Card (if available)
                                        if (message.keyTakeaways.isNotEmpty()) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                                border = CardDefaults.outlinedCardBorder()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "KEY TAKEAWAYS",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    message.keyTakeaways.forEach { takeaway ->
                                                        Row(
                                                            modifier = Modifier.padding(top = 2.dp),
                                                            verticalAlignment = Alignment.Top
                                                        ) {
                                                            Text(
                                                                text = "• ",
                                                                color = MaterialTheme.colorScheme.primary,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Text(
                                                                text = takeaway,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Follow up suggestion chips
                                        if (message.followUpQuestions.isNotEmpty()) {
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = "Follow up ideas:",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .horizontalScroll(rememberScrollState()),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    message.followUpQuestions.forEach { question ->
                                                        SuggestionChip(
                                                            onClick = { handleSend(question) },
                                                            label = { Text(question, fontSize = 12.sp) },
                                                            shape = RoundedCornerShape(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Bottom Action Buttons: Copy & Share
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    copyToClipboard(context, "NovaSearch Answer", message.text)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Copy response",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    shareText(context, "NovaSearch Insight", message.text)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "Share response",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Skeleton Screen while waiting for response
                    if (isLoading) {
                        item {
                            if (isImageMode) {
                                ImageSkeletonScreen(prompt = inputText, style = "Photorealistic", aspectRatio = "1:1")
                            } else {
                                SearchSkeletonScreen(query = inputText, mode = selectedMode)
                            }
                        }
                    }
                }
            }

            // Popover Tool Menu (Matching Screenshot_20260923_200146.jpg)
            if (showToolMenu) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 8.dp)
                        .width(230.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = {
                                isImageMode = true
                                showToolMenu = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isImageMode) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                Text("Create an image", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            }
                        }

                        Surface(
                            onClick = {
                                isImageMode = false
                                selectedMode = "Deep Dive"
                                showToolMenu = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedMode == "Deep Dive" && !isImageMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Outlined.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Text("Think harder", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            }
                        }

                        Surface(
                            onClick = {
                                isImageMode = false
                                selectedMode = "Quick"
                                showToolMenu = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedMode == "Quick" && !isImageMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Outlined.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                                Text("Quick answer", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            }
                        }

                        Surface(
                            onClick = {
                                isImageMode = false
                                selectedMode = "Code & Logic"
                                showToolMenu = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedMode == "Code & Logic" && !isImageMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Outlined.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Text("Code & Logic", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            }
                        }
                    }
                }
            }
        }
    }

    // Fullscreen Dialog for generated images
    fullscreenBitmap?.let { bmp ->
        Dialog(onDismissRequest = { fullscreenBitmap = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Fullscreen Artwork",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Button(
                        onClick = { fullscreenBitmap = null },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}
