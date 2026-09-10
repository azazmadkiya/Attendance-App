package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun CrashScreen(stackTrace: String, onRestart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("App Crashed!", style = MaterialTheme.typography.headlineMedium, color = Color.Red)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRestart) {
            Text("Restart")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stackTrace,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
