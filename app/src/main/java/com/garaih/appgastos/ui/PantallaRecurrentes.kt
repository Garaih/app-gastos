package com.garaih.appgastos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.garaih.appgastos.datos.Categoria
import com.garaih.appgastos.datos.Recurrencia
import com.garaih.appgastos.datos.Recurrente

/** Ajustes > Gastos recurrentes y plazos. */
@Composable
fun PantallaRecurrentes(
    recurrentes: List<Recurrente>,
    categorias: List<Categoria>,
    onEditar: (Recurrente) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activos = recurrentes.filter { it.activo }
    val porClave = categorias.associateBy { it.clave }
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Seccion("Resumen") {
                FilaDato("Fijos al mes", euros(activos.sumOf { it.importeCentimos }), destacado = true)
                val pendiente = activos.sumOf { r -> (Recurrencia.plazosRestantes(r) ?: 0) * r.importeCentimos }
                if (pendiente > 0) FilaDato("Pendiente de compras a plazos", euros(pendiente))
                Text(
                    "Para añadir uno nuevo: + Añadir gasto y activa \"Repetir cada mes\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (recurrentes.isEmpty()) {
            item { Text("Aún no tienes gastos recurrentes.", style = MaterialTheme.typography.bodyMedium) }
        }
        items(recurrentes, key = { it.id }) { r ->
            val cat = r.categoria?.let { porClave[it] }
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onEditar(r) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            (cat?.emoji?.plus(" ") ?: "🔁 ") + r.comercio,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(detalle(r), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        euros(r.importeCentimos),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (r.activo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun detalle(r: Recurrente): String {
    val sig = Recurrencia.siguiente(r)
    val partes = mutableListOf("día ${r.dia}")
    if (r.plazos != null) {
        val restantes = Recurrencia.plazosRestantes(r) ?: 0
        partes += "plazos: ${r.plazos - restantes}/${r.plazos}"
        if (restantes > 0) partes += "quedan $restantes"
    } else partes += "cada mes"
    partes += when {
        !r.activo -> "finalizado"
        sig != null -> "próximo ${sig.fecha.dayOfMonth}/${sig.fecha.monthValue}"
        else -> "terminado"
    }
    return partes.joinToString(" · ")
}

/** Cambiar importe, día o plazos (afecta a los cargos aún no apuntados), finalizar o borrar. */
@Composable
fun DialogoRecurrente(
    r: Recurrente,
    onGuardar: (Recurrente) -> Unit,
    onFinalizar: (Recurrente) -> Unit,
    onBorrar: (Recurrente) -> Unit,
    onCerrar: () -> Unit,
) {
    var comercio by remember { mutableStateOf(r.comercio) }
    var importe by remember { mutableStateOf(importeEditable(r.importeCentimos)) }
    var dia by remember { mutableStateOf(r.dia.toString()) }
    var plazos by remember { mutableStateOf(r.plazos?.toString() ?: "") }
    var confirmarBorrar by remember { mutableStateOf(false) }
    val c = centimosDe(importe)
    val d = dia.toIntOrNull()?.takeIf { it in 1..28 }
    val minimoPlazos = Recurrencia.numeroPlazo(r, Recurrencia.aMes(r.ultimoMes))
    val p = plazos.toIntOrNull()
    val plazosValidos = plazos.isBlank() || (p != null && p >= minimoPlazos)

    if (confirmarBorrar) {
        AlertDialog(
            onDismissRequest = onCerrar,
            title = { Text("¿Borrar \"${r.comercio}\"?") },
            text = { Text("Deja de repetirse. Los gastos ya apuntados se conservan.") },
            confirmButton = { TextButton(onClick = { onBorrar(r); onCerrar() }) { Text("Borrar", color = ROJO) } },
            dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Gasto recurrente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(comercio, { comercio = it }, label = { Text("Comercio o concepto") }, singleLine = true)
                CampoImporte(importe, { importe = it }, "Importe de cada cargo")
                OutlinedTextField(
                    dia, { dia = it.filter { ch -> ch.isDigit() }.take(2) },
                    label = { Text("Día del mes (1-28)") }, singleLine = true, isError = d == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    plazos, { plazos = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Plazos totales (vacío = sin fin)") }, singleLine = true, isError = !plazosValidos,
                    supportingText = { Text("Ya van $minimoPlazos.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Text(
                    "Los cambios afectan a los próximos cargos; los ya apuntados no cambian.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row {
                    if (r.activo) TextButton(onClick = { onFinalizar(r); onCerrar() }) { Text("Finalizar ya") }
                    TextButton(onClick = { confirmarBorrar = true }) { Text("Borrar", color = ROJO) }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = c != null && c > 0 && d != null && plazosValidos && comercio.isNotBlank(),
                onClick = {
                    onGuardar(
                        r.copy(
                            comercio = comercio.trim(), importeCentimos = c!!, dia = d!!,
                            plazos = if (plazos.isBlank()) null else p,
                            activo = true,
                        )
                    )
                    onCerrar()
                },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}
