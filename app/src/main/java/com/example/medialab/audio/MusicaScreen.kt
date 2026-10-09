package com.example.medialab.audio

import android.content.ComponentName
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.medialab.R
import com.example.medialab.video.PlaybackService
import kotlinx.coroutines.delay

@Composable
fun MusicaScreen() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReproductorLocal()
        HorizontalDivider()
        ReproductorSegundoPlano()
    }
}

/** MediaPlayer con un archivo de res/raw, barra de progreso y foco de audio. */
@Composable
private fun ReproductorLocal() {
    val context = LocalContext.current
    // create() hace setDataSource + prepare: el reproductor queda en estado Prepared
    val player = remember { MediaPlayer.create(context, R.raw.cancion) }
    val duracion = remember { player.duration.coerceAtLeast(1) }
    var reproduciendo by remember { mutableStateOf(false) }
    var posicion by remember { mutableStateOf(0) }
    var mensaje by remember { mutableStateOf("") }

    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    val peticionFoco = remember {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { cambio ->
                when (cambio) {
                    // Otra app tomó el audio (o entró una llamada): pausar
                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        if (player.isPlaying) player.pause()
                        reproduciendo = false
                        mensaje = "Pausado: otra app tomó el audio"
                    }
                    // Una notificación corta: solo bajar el volumen
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> player.setVolume(0.2f, 0.2f)
                    AudioManager.AUDIOFOCUS_GAIN -> player.setVolume(1f, 1f)
                }
            }
            .build()
    }

    // Mientras suena, leer la posición 5 veces por segundo para mover la barra
    LaunchedEffect(reproduciendo) {
        while (reproduciendo) {
            posicion = player.currentPosition
            delay(200)
        }
    }

    DisposableEffect(Unit) {
        player.setOnCompletionListener {
            reproduciendo = false
            posicion = 0
            it.seekTo(0)
        }
        onDispose {
            audioManager.abandonAudioFocusRequest(peticionFoco)
            player.release() // siempre liberar al salir de la pantalla
        }
    }

    Text("MediaPlayer · archivo local", style = MaterialTheme.typography.titleMedium)
    Text("${posicion / 1000} s de ${duracion / 1000} s  $mensaje")

    Slider(
        value = posicion.toFloat() / duracion,
        onValueChange = { fraccion ->
            posicion = (fraccion * duracion).toInt()
            player.seekTo(posicion) // milisegundos
        }
    )

    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            if (reproduciendo) {
                player.pause() // Started → Paused
                reproduciendo = false
            } else {
                // Pedir el foco antes de sonar
                val resultado = audioManager.requestAudioFocus(peticionFoco)
                if (resultado == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    player.start() // Prepared/Paused → Started
                    reproduciendo = true
                    mensaje = ""
                } else {
                    mensaje = "No se obtuvo el foco de audio"
                }
            }
        }
    ) { Text(if (reproduciendo) "Pausar" else "Reproducir") }
}

/**
 * Media3 en segundo plano: el reproductor vive en PlaybackService y esta
 * pantalla solo le envía órdenes con un MediaController. Al salir de la app
 * el audio sigue y aparece la notificación con controles.
 */
@Composable
private fun ReproductorSegundoPlano() {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var sonando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val futuro = MediaController.Builder(context, token).buildAsync()
        futuro.addListener(
            {
                val c = futuro.get()
                c.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        sonando = isPlaying
                        if (isPlaying) error = null
                    }

                    override fun onPlayerError(e: PlaybackException) {
                        error = e.errorCodeName // p. ej. sin red
                    }
                })
                sonando = c.isPlaying
                controller = c
            },
            ContextCompat.getMainExecutor(context)
        )
        // Solo se suelta el control; el servicio sigue sonando
        onDispose { MediaController.releaseFuture(futuro) }
    }

    Text("Media3 · segundo plano", style = MaterialTheme.typography.titleMedium)
    Text(
        when {
            controller == null -> "Conectando con el servicio…"
            error != null -> "No se pudo cargar el stream: $error"
            sonando -> "Sonando desde el servicio. Sal de la app: sigue."
            else -> "Stream de audio listo"
        }
    )
    OutlinedButton(
        enabled = controller != null,
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            controller?.let { c ->
                if (c.isPlaying) c.pause() else c.play()
            }
        }
    ) { Text(if (sonando) "Pausar stream" else "Reproducir stream") }
}
