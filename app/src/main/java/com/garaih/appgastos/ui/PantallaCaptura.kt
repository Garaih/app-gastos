package com.garaih.appgastos.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.garaih.appgastos.captura.CapturaRepositorio
import com.garaih.appgastos.captura.Cartera
import com.garaih.appgastos.captura.NotificacionCruda
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PantallaCaptura(
    permisoConcedido: Boolean,
    carteraInstalada: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val notificaciones by CapturaRepositorio.notificaciones.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Captura (spike)",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        item { TarjetaEstado(permisoConcedido, carteraInstalada) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { abrirAjustesPermiso(context) }) {
                    Text(if (permisoConcedido) "Revisar permiso" else "Conceder permiso")
                }
                OutlinedButton(onClick = { CapturaRepositorio.borrar() }) { Text("Vaciar") }
            }
        }
        item {
            Text(
                "Notificaciones de la cartera (${notificaciones.size})",
                style = MaterialTheme.typography.titleMedium
            )
            if (notificaciones.isEmpty()) {
                Text(
                    "Aún no hay ninguna. Paga con el móvil y vuelve aquí. " +
                        "Solo se guardan en memoria mientras la app esté viva.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        items(notificaciones, key = { it.clave + it.momento }) { n -> TarjetaNotificacion(n) }
    }
}

@Composable
private fun TarjetaEstado(permisoConcedido: Boolean, carteraInstalada: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                (if (permisoConcedido) "✅" else "❌") + " Acceso a notificaciones",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                (if (carteraInstalada) "✅" else "⚠️") + " Google Wallet " +
                    if (carteraInstalada) "instalada" else "no encontrada",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                "Paquete filtrado: ${Cartera.PAQUETE_WALLET}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun TarjetaNotificacion(n: NotificacionCruda) {
    val context = LocalContext.current
    val volcado = volcado(n)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectionContainer {
                Text(volcado, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = { copiar(context, volcado) }) { Text("Copiar") }
        }
    }
}

private fun volcado(n: NotificacionCruda): String {
    val fecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(n.momento))
    return buildString {
        appendLine("momento: $fecha")
        appendLine("paquete: ${n.paquete}")
        appendLine("titulo: ${n.titulo}")
        appendLine("texto: ${n.texto}")
        appendLine("textoGrande: ${n.textoGrande}")
        appendLine("subTexto: ${n.subTexto}")
        append("textoInfo: ${n.textoInfo}")
    }
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
