package com.example.medialab.audio

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
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
import com.example.medialab.R
import kotlinx.coroutines.delay

private const val ESPERA_DEMO_MS = 3_000L

/** SoundPool: efectos cortos, cargados en memoria, que suenan al instante. */
@Composable
fun SonidosScreen() {
    val context = LocalContext.current
    var cargados by remember { mutableStateOf(0) }

    val pool = remember {
        SoundPool.Builder()
            .setMaxStreams(4) // hasta 4 sonidos al mismo tiempo
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
            .also { p ->
                // load() es asíncrono: hay que esperar este aviso antes de play()
                p.setOnLoadCompleteListener { _, _, status ->
                    if (status == 0) cargados++
                }
            }
    }
    val clickId = remember { pool.load(context, R.raw.click, 1) }
    val campanaId = remember { pool.load(context, R.raw.campana, 1) }
    // Solo para la demo: la carga real tarda milisegundos, así que se retiene
    // el estado "Cargando…" unos segundos para que se alcance a ver
    var esperaDemo by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(ESPERA_DEMO_MS)
        esperaDemo = false
    }
    val listo = cargados >= 2 && !esperaDemo

    DisposableEffect(Unit) {
        onDispose { pool.release() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (listo) "Sonidos cargados en memoria" else "Cargando sonidos…")

        Button(
            enabled = listo,
            modifier = Modifier.fillMaxWidth(),
            // id, volIzq, volDer, prioridad, repeticiones, velocidad
            onClick = { pool.play(clickId, 1f, 1f, 1, 0, 1f) }
        ) { Text("Click") }

        Button(
            enabled = listo,
            modifier = Modifier.fillMaxWidth(),
            onClick = { pool.play(campanaId, 1f, 1f, 1, 0, 1f) }
        ) { Text("Campana") }

        Button(
            enabled = listo,
            modifier = Modifier.fillMaxWidth(),
            onClick = { pool.play(campanaId, 1f, 1f, 1, 0, 2f) } // velocidad 2x = más agudo
        ) { Text("Campana a velocidad 2x") }

        Button(
            enabled = listo,
            modifier = Modifier.fillMaxWidth(),
            onClick = { pool.play(clickId, 1f, 1f, 1, 3, 1f) } // 3 repeticiones extra
        ) { Text("Click repetido 4 veces") }

        Button(
            enabled = listo,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                // Dos streams simultáneos: algo que un solo MediaPlayer no hace
                pool.play(campanaId, 1f, 1f, 1, 0, 1f)
                pool.play(campanaId, 1f, 1f, 1, 0, 1.5f)
            }
        ) { Text("Dos campanas a la vez") }
    }
}
