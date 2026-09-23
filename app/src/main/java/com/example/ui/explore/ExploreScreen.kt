package com.example.ui.explore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PromptTemplate(
    val category: String,
    val title: String,
    val description: String,
    val prompt: String,
    val isImage: Boolean = false,
    val style: String? = null
)

@Composable
fun ExploreScreen(
    onRunSearch: (String) -> Unit,
    onRunImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf("All", "Search & Q&A", "AI Art & Design", "Code & Dev", "Science", "Productivity")
    var selectedCategory by remember { mutableStateOf("All") }

    val promptTemplates = listOf(
        // Search & Q&A
        PromptTemplate(
            category = "Search & Q&A",
            title = "Quantum Computing in Plain English",
            description = "Get a conceptual breakdown of qubits, superposition, and entanglement without jargon.",
            prompt = "Explain quantum computing and superposition as if I am 12 years old, using a coin spinning analogy."
        ),
        PromptTemplate(
            category = "Search & Q&A",
            title = "Zero-Knowledge Proofs",
            description = "Understand cryptographic privacy and verifiable computation.",
            prompt = "What are Zero-Knowledge Proofs (ZKPs) and why are they critical for digital privacy and blockchain?"
        ),
        PromptTemplate(
            category = "Search & Q&A",
            title = "Clean Energy Storage Solutions",
            description = "Compare emerging grid-scale batteries, flow batteries, and pumped hydro storage.",
            prompt = "Compare the top next-generation energy storage technologies for renewable energy grids."
        ),

        // AI Art & Design
        PromptTemplate(
            category = "AI Art & Design",
            title = "Neon Cyberpunk Tokyo Cafe",
            description = "Rainy street, holographic lanterns, cozy interior lighting, anime vibes.",
            prompt = "Cyberpunk Neo-Tokyo ramen bar on a rainy night, glowing neon reflections on wet asphalt, steam rising, cozy warm amber interior",
            isImage = true,
            style = "Cyberpunk"
        ),
        PromptTemplate(
            category = "AI Art & Design",
            title = "Crystalline Phoenix Aurora",
            description = "Mythical bird made of stained glass and ice soaring through polar lights.",
            prompt = "Majestic mythical phoenix sculpted from luminescent crystal and aurora borealis ribbons, cosmic starry background, 8k",
            isImage = true,
            style = "Fantasy Art"
        ),
        PromptTemplate(
            category = "AI Art & Design",
            title = "Isometric 3D Programmer Desk",
            description = "Clean aesthetic setup with miniature plants, ultrawide screen, and warm glow.",
            prompt = "Isometric 3D diorama of a modern minimal developer workspace with bonsai trees, soft pastel lighting, clay render",
            isImage = true,
            style = "3D Render"
        ),

        // Code & Dev
        PromptTemplate(
            category = "Code & Dev",
            title = "Kotlin Coroutines & Flow Architecture",
            description = "Best practices for cold vs hot flows, StateFlow, and SharedFlow in Compose.",
            prompt = "Explain StateFlow vs SharedFlow in Kotlin Coroutines with clear architecture code examples for Jetpack Compose."
        ),
        PromptTemplate(
            category = "Code & Dev",
            title = "Clean Architecture in Mobile Apps",
            description = "Domain models, Use Cases, Repositories, and ViewModels separation of concerns.",
            prompt = "Explain how to structure an Android app using Clean Architecture and MVI/MVVM pattern with diagrammatic code."
        ),

        // Science
        PromptTemplate(
            category = "Science",
            title = "James Webb Telescope Discoveries",
            description = "Key cosmological findings about early galaxies and exoplanet atmospheres.",
            prompt = "What are the most revolutionary discoveries made by the James Webb Space Telescope so far?"
        ),
        PromptTemplate(
            category = "Science",
            title = "Epigenetics & Cellular Longevity",
            description = "How lifestyle and environment influence gene expression and biological aging.",
            prompt = "How does epigenetics work, and what are scientifically validated habits that influence biological age?"
        ),

        // Productivity
        PromptTemplate(
            category = "Productivity",
            title = "Time-Blocking Framework",
            description = "Actionable schedule for balancing deep creative work and reactive communications.",
            prompt = "Design a high-productivity daily time-blocking schedule for deep work, minimizing cognitive fatigue."
        ),
        PromptTemplate(
            category = "Productivity",
            title = "First Principles Problem Solving",
            description = "Deconstruct complicated challenges to fundamental truths.",
            prompt = "Explain the First Principles thinking method popularized by Elon Musk and Aristotle with a practical case study."
        )
    )

    val filteredPrompts = remember(selectedCategory) {
        if (selectedCategory == "All") promptTemplates
        else promptTemplates.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Prompt Sparks & Inspiration",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Tap any spark to instantly launch an intelligent AI search or generate custom art.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Category Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        items(filteredPrompts, key = { it.title }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (item.isImage) onRunImage(item.prompt)
                        else onRunSearch(item.prompt)
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (item.isImage) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (item.isImage) "🎨 AI ART" else "⚡ SEARCH",
                                color = if (item.isImage) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Icon(
                            imageVector = if (item.isImage) Icons.Default.Brush else Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (item.isImage) "Generate Art →" else "Ask AI Search →",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (item.isImage) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    }
}
