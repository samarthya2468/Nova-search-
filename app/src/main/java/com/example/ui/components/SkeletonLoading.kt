package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Creates an animated linear shimmer brush that smoothly sweeps across skeleton elements
 */
@Composable
fun shimmerBrush(
    showShimmer: Boolean = true,
    targetValue: Float = 1300f
): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
        val transition = rememberInfiniteTransition(label = "shimmer_transition")
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer_translate"
        )
        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnimation.value - 400f, translateAnimation.value - 400f),
            end = Offset(translateAnimation.value, translateAnimation.value)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}

/**
 * Basic skeleton shape box with shimmer animation
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val brush = shimmerBrush()
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

/**
 * High-fidelity skeleton screen for AI Search responses
 */
@Composable
fun SearchSkeletonScreen(
    query: String,
    mode: String,
    modifier: Modifier = Modifier
) {
    var phaseIndex by remember { mutableIntStateOf(0) }
    val phases = remember {
        listOf(
            "Connecting to Gemini 3.5 Flash...",
            "Analyzing query & context...",
            "Synthesizing structured answer...",
            "Extracting key takeaways & related queries..."
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1600)
            phaseIndex = (phaseIndex + 1) % phases.size
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("search_skeleton_screen"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: mode badge & query
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            text = "${mode.uppercase()} SYNTHESIS",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SkeletonBox(modifier = Modifier.size(28.dp), shape = CircleShape)
                        SkeletonBox(modifier = Modifier.size(28.dp), shape = CircleShape)
                    }
                }

                Text(
                    text = query.ifBlank { "Searching..." },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Indeterminate Progress Indicator with status text
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .testTag("search_indeterminate_progress"),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
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
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = phases[phaseIndex],
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "AI Thinking",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Paragraph Skeleton Lines
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SkeletonBox(modifier = Modifier.fillMaxWidth(0.95f).height(14.dp))
                SkeletonBox(modifier = Modifier.fillMaxWidth(0.98f).height(14.dp))
                SkeletonBox(modifier = Modifier.fillMaxWidth(0.85f).height(14.dp))
                SkeletonBox(modifier = Modifier.fillMaxWidth(0.65f).height(14.dp))
            }

            // Sub-section Header Skeleton
            SkeletonBox(modifier = Modifier.fillMaxWidth(0.4f).height(18.dp))

            // Bullet List Skeletons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SkeletonBox(modifier = Modifier.size(8.dp), shape = CircleShape)
                    SkeletonBox(modifier = Modifier.fillMaxWidth(0.92f).height(12.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SkeletonBox(modifier = Modifier.size(8.dp), shape = CircleShape)
                    SkeletonBox(modifier = Modifier.fillMaxWidth(0.88f).height(12.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SkeletonBox(modifier = Modifier.size(8.dp), shape = CircleShape)
                    SkeletonBox(modifier = Modifier.fillMaxWidth(0.75f).height(12.dp))
                }
            }

            // Key Takeaways Card Skeleton
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SkeletonBox(modifier = Modifier.size(18.dp), shape = CircleShape)
                        SkeletonBox(modifier = Modifier.width(120.dp).height(14.dp))
                    }
                    SkeletonBox(modifier = Modifier.fillMaxWidth(0.9f).height(12.dp))
                    SkeletonBox(modifier = Modifier.fillMaxWidth(0.82f).height(12.dp))
                    SkeletonBox(modifier = Modifier.fillMaxWidth(0.7f).height(12.dp))
                }
            }

            // Follow-up Questions Skeleton Pills
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBox(modifier = Modifier.width(110.dp).height(12.dp))
                SkeletonBox(modifier = Modifier.fillMaxWidth().height(38.dp), shape = RoundedCornerShape(10.dp))
                SkeletonBox(modifier = Modifier.fillMaxWidth().height(38.dp), shape = RoundedCornerShape(10.dp))
            }
        }
    }
}

/**
 * High-fidelity skeleton screen for AI Image Generation
 */
@Composable
fun ImageSkeletonScreen(
    prompt: String,
    style: String,
    aspectRatio: String,
    modifier: Modifier = Modifier
) {
    var phaseIndex by remember { mutableIntStateOf(0) }
    val phases = remember {
        listOf(
            "Analyzing prompt & style aesthetics...",
            "Composing volumetric lighting & geometry...",
            "Rendering visual canvas with Gemini 2.5 Flash Image...",
            "Finalizing image details..."
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1700)
            phaseIndex = (phaseIndex + 1) % phases.size
        }
    }

    // Pulse animation for center icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("image_skeleton_screen"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Style tag and actions
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
                        text = "$style • $aspectRatio",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SkeletonBox(modifier = Modifier.size(28.dp), shape = CircleShape)
                    SkeletonBox(modifier = Modifier.size(28.dp), shape = CircleShape)
                }
            }

            // Aspect-Ratio-Matched Shimmering Canvas Placeholder
            val ratioFloat = when (aspectRatio) {
                "16:9" -> 16f / 9f
                "9:16" -> 9f / 16f
                "4:3" -> 4f / 3f
                else -> 1f
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(ratioFloat)
                    .clip(RoundedCornerShape(14.dp))
                    .background(shimmerBrush())
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Central glowing icon and loading pulse
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size((56 * iconScale).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size((28 * iconScale).dp)
                        )
                    }
                    Text(
                        text = "Synthesizing Canvas",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Indeterminate Progress Bar & Animated Phase Description
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .testTag("image_indeterminate_progress"),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
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
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = phases[phaseIndex],
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "Rendering",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Prompt preview
            Text(
                text = "\"$prompt\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
