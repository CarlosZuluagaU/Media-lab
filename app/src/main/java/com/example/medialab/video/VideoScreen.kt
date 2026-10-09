package com.example.medialab.video

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

// Videos de prueba públicos. Se pueden cambiar por cualquier .m3u8 o .mp4.
private const val VIDEO_HLS =
    "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8"
private const val VIDEO_MP4 =
    "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"

/** Media3 ExoPlayer: lista con un stream HLS y un MP4, estados y errores. */
@Composable
fun VideoScreen() {
    val context = LocalContext.current
    var estado by remember { mutableStateOf("Preparando…") }
    var numero by remember { mutableStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }

    // remember: un solo reproductor aunque la pantalla se recomponga
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItems(
                listOf(
                    MediaItem.fromUri(VIDEO_HLS), // HLS adaptativo
                    MediaItem.fromUri(VIDEO_MP4)  // MP4 progresivo
                )
            )
            prepare()            // no bloquea el hilo principal
            playWhenReady = true // arranca cuando haya búfer
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                estado = when (state) {
                    Player.STATE_BUFFERING -> "Cargando…"
                    Player.STATE_READY -> "Listo"
                    Player.STATE_ENDED -> "Terminó la lista"
                    else -> "Detenido"
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                numero = player.currentMediaItemIndex + 1
            }

            override fun onPlayerError(e: PlaybackException) {
                error = e.errorCodeName // p. ej. sin red o formato no soportado
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release() // liberar al salir de la pantalla
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // PlayerView es una vista clásica: se envuelve con AndroidView
        AndroidView(
            factory = { PlayerView(it).apply { this.player = player } },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16 / 9f)
        )

        Text("Video $numero de 2 · Estado: $estado")

        error?.let { codigo ->
            Text("Error: $codigo", color = MaterialTheme.colorScheme.error)
            Button(onClick = {
                error = null
                player.prepare() // reintentar después de un error
                player.play()
            }) { Text("Reintentar") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // play() por si estaba en pausa: al cambiar de video arranca solo
            OutlinedButton(onClick = {
                player.seekToPreviousMediaItem()
                player.play()
            }) { Text("Anterior") }
            OutlinedButton(onClick = {
                player.seekToNextMediaItem()
                player.play()
            }) { Text("Siguiente") }
        }
    }
}
