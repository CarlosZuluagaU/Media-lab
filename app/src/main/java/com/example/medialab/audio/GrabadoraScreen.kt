package com.example.medialab.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
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
import androidx.core.content.ContextCompat
import java.io.File

/** El constructor con context existe desde Android 12 (API 31). */
@Suppress("DEPRECATION")
private fun crearRecorder(context: Context): MediaRecorder =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()

/** MediaRecorder: graba el micrófono a un .m4a y lo reproduce con MediaPlayer. */
@Composable
fun GrabadoraScreen() {
    val context = LocalContext.current
    // Almacenamiento privado de la app: no necesita permisos de archivos
    val archivo = remember { File(context.filesDir, "nota.m4a") }

    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var hayNota by remember { mutableStateOf(archivo.exists()) }
    var mensaje by remember { mutableStateOf("Listo para grabar") }

    fun iniciarGrabacion() {
        player?.release()
        player = null
        val r = crearRecorder(context)
        try {
            // El orden es obligatorio: fuente, formato, códec, archivo, prepare, start
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(44_100)
            r.setAudioEncodingBitRate(128_000)
            r.setOutputFile(archivo.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            mensaje = "Grabando…"
        } catch (e: Exception) {
            r.release()
            mensaje = "No se pudo grabar: ${e.message}"
        }
    }

    fun detenerGrabacion() {
        val r = recorder ?: return
        try {
            r.stop()
            mensaje = "Nota guardada en ${archivo.name}"
        } catch (e: RuntimeException) {
            // stop() falla si se detiene casi de inmediato (no alcanzó a grabar nada)
            mensaje = "Grabación demasiado corta"
        }
        r.release()
        recorder = null
        hayNota = archivo.exists() && archivo.length() > 0
    }

    fun reproducirNota() {
        player?.release()
        try {
            val p = MediaPlayer()
            p.setDataSource(archivo.absolutePath)
            p.setOnCompletionListener {
                it.release()
                player = null
                mensaje = "Fin de la nota"
            }
            p.prepare() // archivo local y pequeño: prepare() síncrono está bien
            p.start()
            player = p
            mensaje = "Reproduciendo la nota…"
        } catch (e: Exception) {
            mensaje = "No se pudo reproducir: ${e.message}"
        }
    }

    // Permiso peligroso: además del manifiesto se pide en tiempo de ejecución
    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) iniciarGrabacion() else mensaje = "Sin permiso de micrófono no se puede grabar"
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.release()
            player?.release()
        }
    }

    val grabando = recorder != null

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(mensaje)

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (grabando) {
                    detenerGrabacion()
                } else {
                    val estado = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    )
                    if (estado == PackageManager.PERMISSION_GRANTED) iniciarGrabacion()
                    else pedirPermiso.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        ) { Text(if (grabando) "Detener grabación" else "Grabar") }

        OutlinedButton(
            enabled = hayNota && !grabando,
            modifier = Modifier.fillMaxWidth(),
            onClick = { reproducirNota() }
        ) { Text("Reproducir la nota") }
    }
}
