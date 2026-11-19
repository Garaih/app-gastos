package com.garaih.appgastos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.garaih.appgastos.BuildConfig
import com.garaih.appgastos.actualizacion.Actualizador
import kotlinx.coroutines.launch

private sealed interface Estado {
    data object Buscando : Estado
    data class Resultado(val r: Actualizador.Resultado) : Estado
    data class Descargando(val version: Actualizador.Version) : Estado
    data class Listo(val version: Actualizador.Version, val apk: java.io.File) : Estado
    data class Fallo(val mensaje: String) : Estado
}

@Composable
fun DialogoActualizar(onCerrar: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var estado by remember { mutableStateOf<Estado>(Estado.Buscando) }
    var progreso by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) { estado = Estado.Resultado(Actualizador.buscar(context)) }

    fun instalarSiSePuede(e: Estado.Listo) {
        if (Actualizador.puedeInstalar(context)) Actualizador.instalar(context, e.apk)
        else Actualizador.abrirPermisoInstalar(context)
    }

    val actual = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Actualizaciones") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (val e = estado) {
                    Estado.Buscando -> {
                        CircularProgressIndicator()
                        Text("Buscando la última versión…")
                    }
                    is Estado.Resultado -> when (val r = e.r) {
                        is Actualizador.Resultado.AlDia -> Text("✅ Tienes la última versión.\n\nVersión instalada: $actual")
                        is Actualizador.Resultado.Error -> Text("No se pudo comprobar: ${r.mensaje}\n\nVersión instalada: $actual")
                        is Actualizador.Resultado.Disponible -> {
                            Text("Hay una versión nueva: ${r.version.nombre}", style = MaterialTheme.typography.titleSmall)
                            Text("Instalada: $actual", style = MaterialTheme.typography.bodySmall)
                            if (r.version.notas.isNotBlank()) {
                                Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                                    Text(r.version.notas, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    is Estado.Descargando -> {
                        Text("Descargando ${e.version.nombre}…")
                        if (progreso >= 0f) LinearProgressIndicator(progress = { progreso }, modifier = Modifier.fillMaxWidth())
                        else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    is Estado.Listo -> Text(
                        if (Actualizador.puedeInstalar(context))
                            "Descargada. Pulsa Instalar y confirma en la ventana de Android. Tus datos se conservan."
                        else
                            "Descargada. Antes, permite que \"Gastos\" instale aplicaciones (solo una vez): " +
                                "pulsa Dar permiso, activa la opción, vuelve y pulsa Instalar."
                    )
                    is Estado.Fallo -> Text("Error: ${e.mensaje}")
                }
            }
        },
        confirmButton = {
            when (val e = estado) {
                is Estado.Resultado -> (e.r as? Actualizador.Resultado.Disponible)?.let { d ->
                    TextButton(onClick = {
                        estado = Estado.Descargando(d.version)
                        scope.launch {
                            estado = try {
                                Estado.Listo(d.version, Actualizador.descargar(context, d.version) { progreso = it })
                            } catch (ex: Exception) {
                                Estado.Fallo(ex.message ?: "descarga fallida")
                            }
                        }
                    }) { Text("Descargar") }
                }
                is Estado.Listo -> TextButton(onClick = { instalarSiSePuede(e) }) {
                    Text(if (Actualizador.puedeInstalar(context)) "Instalar" else "Dar permiso")
                }
                else -> Unit
            }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } },
    )
}
