package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SearchHistoryEntity
import com.example.data.repository.ImageRepository
import com.example.data.repository.SearchRepository
import com.example.ui.chat.GeminiChatScreen
import com.example.ui.components.GeminiChatDrawer
import com.example.ui.components.SettingsBottomSheet
import com.example.ui.explore.ExploreScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.image.ImageGeneratorScreen
import kotlinx.coroutines.launch

enum class AppScreen {
    CHAT,
    IMAGE_STUDIO,
    EXPLORE,
    HISTORY
}

@Composable
fun NovaSearchApp(
    searchRepository: SearchRepository,
    imageRepository: ImageRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var currentScreen by remember { mutableStateOf(AppScreen.CHAT) }
    var activeChatEntity by remember { mutableStateOf<SearchHistoryEntity?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentScreen == AppScreen.CHAT,
        drawerContent = {
            GeminiChatDrawer(
                searchRepository = searchRepository,
                currentChatId = activeChatEntity?.id,
                onSelectChat = { entity ->
                    activeChatEntity = entity
                    currentScreen = AppScreen.CHAT
                    coroutineScope.launch { drawerState.close() }
                },
                onNewChat = {
                    activeChatEntity = null
                    currentScreen = AppScreen.CHAT
                    coroutineScope.launch { drawerState.close() }
                },
                onNavigateToImages = {
                    currentScreen = AppScreen.IMAGE_STUDIO
                    coroutineScope.launch { drawerState.close() }
                },
                onNavigateToSparks = {
                    currentScreen = AppScreen.EXPLORE
                    coroutineScope.launch { drawerState.close() }
                },
                onOpenSettings = {
                    showSettingsSheet = true
                    coroutineScope.launch { drawerState.close() }
                }
            )
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("app_scaffold")
    ) {
        when (currentScreen) {
            AppScreen.CHAT -> {
                GeminiChatScreen(
                    searchRepository = searchRepository,
                    imageRepository = imageRepository,
                    activeChatEntity = activeChatEntity,
                    onOpenDrawer = {
                        coroutineScope.launch { drawerState.open() }
                    },
                    onNewChat = {
                        activeChatEntity = null
                    },
                    onOpenSettings = {
                        showSettingsSheet = true
                    }
                )
            }

            AppScreen.IMAGE_STUDIO -> {
                Scaffold(
                    topBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { currentScreen = AppScreen.CHAT },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Chat",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Image Studio & Art Gallery",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                ) { innerPadding ->
                    ImageGeneratorScreen(
                        repository = imageRepository,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }

            AppScreen.EXPLORE -> {
                Scaffold(
                    topBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { currentScreen = AppScreen.CHAT },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Chat",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Prompt Sparks & Exploration",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                ) { innerPadding ->
                    ExploreScreen(
                        onRunSearch = { prompt ->
                            activeChatEntity = null
                            currentScreen = AppScreen.CHAT
                        },
                        onRunImage = { prompt ->
                            currentScreen = AppScreen.IMAGE_STUDIO
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }

            AppScreen.HISTORY -> {
                Scaffold(
                    topBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { currentScreen = AppScreen.CHAT },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to Chat",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Search & Image History",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                ) { innerPadding ->
                    HistoryScreen(
                        searchRepository = searchRepository,
                        imageRepository = imageRepository,
                        onSelectSearch = { query ->
                            activeChatEntity = null
                            currentScreen = AppScreen.CHAT
                        },
                        onSelectImagePrompt = { prompt ->
                            currentScreen = AppScreen.IMAGE_STUDIO
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    if (showSettingsSheet) {
        SettingsBottomSheet(
            onDismiss = { showSettingsSheet = false },
            onKeySaved = {
                showSettingsSheet = false
            }
        )
    }
}
