package com.example.grocery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.grocery.data.GroceryRepository
import com.example.grocery.ui.GroceryListScreen
import com.example.grocery.ui.theme.GroceryListTheme

class MainActivity : ComponentActivity() {
    private val repository = GroceryRepository() // In a real app, use Hilt/Koin injection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GroceryListTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    GroceryListScreen(repository = repository)
                }
            }
        }
    }
}
