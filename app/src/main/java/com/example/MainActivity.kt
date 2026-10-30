package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.UnbanRepository
import com.example.ui.MainScreen
import com.example.ui.UnbanViewModel
import com.example.ui.UnbanViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val database = AppDatabase.getDatabase(applicationContext)
    val repository = UnbanRepository(database.unbanRequestDao())
    val factory = UnbanViewModelFactory(repository)
    val viewModel = ViewModelProvider(this, factory)[UnbanViewModel::class.java]

    setContent {
      MyApplicationTheme {
        MainScreen(viewModel = viewModel)
      }
    }
  }
}

