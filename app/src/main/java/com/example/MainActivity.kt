package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.data.repository.ImageRepository
import com.example.data.repository.SearchRepository
import com.example.ui.NovaSearchApp
import com.example.ui.theme.NovaSearchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val searchRepository = remember { SearchRepository(applicationContext) }
            val imageRepository = remember { ImageRepository(applicationContext) }

            NovaSearchTheme {
                NovaSearchApp(
                    searchRepository = searchRepository,
                    imageRepository = imageRepository,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// Retained for backward-compatibility with existing screenshot tests
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NovaSearchTheme { Greeting("Android") }
}
