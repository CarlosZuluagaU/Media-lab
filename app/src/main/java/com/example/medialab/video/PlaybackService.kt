package com.example.medialab.video

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * El reproductor vive aquí y no en la pantalla, por eso sigue sonando
 * con la app cerrada. Media3 crea sola la notificación con controles.
 */
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            // true = Media3 maneja el foco de audio por nosotros
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .build()
        player.setMediaItem(
            MediaItem.Builder()
                .setUri(AUDIO_URL)
                // Título y artista que muestra la notificación
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Stream de prueba")
                        .setArtist("MediaLab")
                        .build()
                )
                .build()
        )
        player.prepare()
        session = MediaSession.Builder(this, player).build()
    }

    // La interfaz (MediaController) y el sistema se conectan a esta sesión
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    // Si el usuario cierra la app desde recientes y no está sonando, apagar el servicio
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady) stopSelf()
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    companion object {
        // Audio de prueba público del proyecto ExoPlayer. Se puede cambiar por otro MP3 o stream.
        const val AUDIO_URL = "https://storage.googleapis.com/exoplayer-test-media-0/play.mp3"
    }
}
