package com.ricardo.bookreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ricardo.bookreader.navigation.BookReaderApp
import com.ricardo.bookreader.ui.theme.BookReaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BookReaderTheme {
                BookReaderApp()
            }
        }
    }
}
