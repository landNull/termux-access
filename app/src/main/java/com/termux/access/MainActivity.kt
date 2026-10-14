package com.termux.access

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

// Minimal UI that starts and stops the WebDAV foreground service.
// The server remains off until the user explicitly taps the button.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen(
                    onStartServer = { startService(Intent(this, WebDavServerService::class.java)) },
                    onStopServer = { startService(Intent(this, WebDavServerService::class.java).setAction(WebDavServerService.ACTION_STOP)) }
                )
            }
        }
    }
}

// Renders the toggle and a short status line about the Apps folder.
@Composable
fun MainScreen(onStartServer: () -> Unit, onStopServer: () -> Unit) {
    var running by remember { mutableStateOf(false) }
    val appsExists = File("/data/data/com.termux/files/home/Apps").exists()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Termux:Access", style = MaterialTheme.typography.headlineMedium)
        Text(if (appsExists) "Apps folder: Found" else "Apps folder: Not found (mkdir -p ~/Apps in Termux)")
        Text("Network server is off by default. Intended for trusted LAN only.")
        Button(onClick = {
            if (running) onStopServer() else onStartServer()
            running = !running
        }) {
            Text(if (running) "Stop WebDAV Server" else "Start WebDAV Server")
        }
        Text("SAF grant management and full WebDAV methods are in the provider and service layers.")
    }
}
