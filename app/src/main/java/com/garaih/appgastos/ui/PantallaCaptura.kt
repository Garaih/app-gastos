package com.garaih.appgastos.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.garaih.appgastos.captura.CapturaRepositorio
import com.garaih.appgastos.captura.Cartera
import com.garaih.appgastos.captura.NotificacionCruda
import com.garaih.appgastos.datos.GastosRepositorio
import kotlinx.coroutines.launch

/** Ajustes > Permisos: estado del acceso a notificaciones y de Google Wallet. */
@Composable
fun PantallaPermisos(
    permisoConcedido: Boolean,
    carteraInstalada: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    (if (permisoConcedido) "✅" else "❌") + "  Acceso a notificaciones",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Necesario para registrar solos los pagos con el móvil. " +
                        "La app solo lee las notificaciones de Google Wallet e ignora el resto. " +
                        "Nada sale del teléfono.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = { abrirAjustesPermiso(context) }) {
                    Text(if (permisoConcedido) "Revisar en ajustes" else "Conceder permiso")
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    (if (carteraInstalada) "✅" else "⚠️") + "  Google Wallet " +
                        if (carteraInstalada) "instalada" else "no encontrada",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Paquete: ${Cartera.PAQUETE_WALLET}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            "En Xiaomi: activa también \"Inicio automático\" y pon el ahorro de batería de la app en \"Sin restricciones\".",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Ajustes > Diagnóstico: texto crudo de las notificaciones capturadas y datos de ejemplo. */
@Composable
fun PantallaDiagnostico(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val notificaciones by CapturaRepositorio.notificaciones.collectAsState()
    val repositorio = remember { GastosRepositorio.de(context) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Notificaciones de la cartera recibidas desde que se abrió la app. Solo en memoria.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { CapturaRepositorio.borrar() }) { Text("Vaciar") }
                OutlinedButton(onClick = { scope.launch { repositorio.insertarEjemplo() } }) { Text("Insertar ejemplo") }
                OutlinedButton(onClick = { scope.launch { repositorio.borrarPruebas() } }) { Text("Borrar ejemplos") }
            }
        }
        if (notificaciones.isEmpty()) {
            item { Text("Aún no hay ninguna.", style = MaterialTheme.typography.bodyMedium) }
        }
        items(notificaciones, key = { it.clave + it.momento }) { n -> TarjetaNotificacion(n) }
    }
}

@Composable
private fun TarjetaNotificacion(n: NotificacionCruda) {
    val context = LocalContext.current
    val volcado = volcado(n)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectionContainer {
                Text(volcado, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = { copiar(context, volcado) }) { Text("Copiar") }
        }
    }
}

private fun volcado(n: NotificacionCruda): String = buildString {
    appendLine("momento: ${fechaHora(n.momento)}")
    appendLine("paquete: ${n.paquete}")
    appendLine("titulo: ${n.titulo}")
    appendLine("texto: ${n.texto}")
    appendLine("textoGrande: ${n.textoGrande}")
    appendLine("subTexto: ${n.subTexto}")
    append("textoInfo: ${n.textoInfo}")
}

private fun copiar(context: Context, texto: String) {
    val cm = context.getSystemService(ClipboardManager::class.java)
    cm.setPrimaryClip(ClipData.newPlainText("notificacion", texto))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show()
    }
}

private fun abrirAjustesPermiso(context: Context) {
    try {
        context.startActivity(Cartera.intentAjustesPermiso(context))
    } catch (e: Exception) {
        context.startActivity(Cartera.intentAjustesPermisoGeneral())
    }
}
