package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.firebase.FirebaseService
import com.example.ui.navigation.WhisperNavGraph
import com.example.ui.theme.WhisperTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialConversationId = intent?.getStringExtra("EXTRA_CONVERSATION_ID")

        setContent {
            WhisperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WhisperNavGraph(initialConversationId = initialConversationId)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        FirebaseService.updatePresence(true)
    }

    override fun onPause() {
        super.onPause()
        FirebaseService.updatePresence(false)
    }
}
