package com.example.medialab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.medialab.audio.GrabadoraScreen
import com.example.medialab.audio.MusicaScreen
import com.example.medialab.audio.SonidosScreen
import com.example.medialab.video.VideoScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MediaLabApp()
            }
        }
    }
}

/** Una pantalla por cada API de la exposición. */
@Composable
fun MediaLabApp() {
    var tab by rememberSaveable { mutableStateOf(0) }
    val titulos = listOf("Sonidos", "Grabadora", "Música", "Video")
    val iconos = listOf("🔔", "🎙", "🎵", "🎬")

    Scaffold(
        bottomBar = {
            NavigationBar {
                titulos.forEachIndexed { i, titulo ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(iconos[i]) },
                        label = { Text(titulo) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(titulos[tab], style = MaterialTheme.typography.headlineMedium)
            // Al cambiar de pestaña la pantalla anterior sale de composición
            // y su DisposableEffect libera el reproductor o el grabador.
            when (tab) {
                0 -> SonidosScreen()
                1 -> GrabadoraScreen()
                2 -> MusicaScreen()
                else -> VideoScreen()
            }
        }
    }
}
