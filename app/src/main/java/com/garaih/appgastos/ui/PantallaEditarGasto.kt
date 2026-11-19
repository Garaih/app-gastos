package com.garaih.appgastos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.garaih.appgastos.datos.Categoria
import com.garaih.appgastos.datos.Gasto
import com.garaih.appgastos.datos.ParserCartera
import com.garaih.appgastos.datos.Recurrencia
import com.garaih.appgastos.datos.Recurrente
import androidx.compose.material3.Switch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Alta (gasto == null) o edición. onGuardar recibe el gasto y si recordar la categoría para el comercio.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PantallaEditarGasto(
    gasto: Gasto?,
    categorias: List<Categoria>,
    recurrente: Recurrente?,
    /** (gasto, recordar categoría, repetición: null = no repetir; Repeticion(plazos=null) = cada mes sin fin) */
    onGuardar: (Gasto, Boolean, Repeticion?) -> Unit,
    onGestionarRecurrente: (Recurrente) -> Unit,
    onBorrar: (Gasto) -> Unit,
    mostrarConfirmarBorrado: Boolean,
    onCerrarConfirmarBorrado: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var importe by remember { mutableStateOf(importeEditable(gasto?.importeCentimos)) }
    var comercio by remember { mutableStateOf(gasto?.comercio?.takeIf { it != "Sin interpretar" } ?: "") }
    var fecha by remember { mutableStateOf(gasto?.fecha ?: System.currentTimeMillis()) }
    var categoria by remember { mutableStateOf(gasto?.categoria) }
    var subcategoria by remember { mutableStateOf(gasto?.subcategoria) }
    var recordar by remember { mutableStateOf(true) }
    var verCalendario by remember { mutableStateOf(false) }
    var repetir by remember { mutableStateOf(false) }
    var plazosTexto by remember { mutableStateOf("") }
    val plazos = plazosTexto.toIntOrNull()?.takeIf { it in 2..360 }

    val centimos = ParserCartera.aCentimos(importe.trim())
    val importeValido = centimos != null && centimos > 0
    val valido = importeValido && comercio.isNotBlank()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (gasto != null && !gasto.interpretado && gasto.textoCrudo != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("No se pudo leer el importe. Texto recibido:", style = MaterialTheme.typography.labelLarge)
                    Text(gasto.textoCrudo, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        OutlinedTextField(
            value = importe,
            onValueChange = { nuevo -> importe = nuevo.filter { it.isDigit() || it == ',' || it == '.' } },
            label = { Text("Importe") },
            suffix = { Text("€") },
            isError = importe.isNotEmpty() && !importeValido,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = comercio,
            onValueChange = { comercio = it },
            label = { Text("Comercio o concepto") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedButton(onClick = { verCalendario = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.DateRange, contentDescription = null)
            Text("  " + fechaCorta(fecha))
        }

        Text("Categoría", style = MaterialTheme.typography.titleSmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            categorias.forEach { c ->
                FilterChip(
                    selected = categoria == c.clave,
                    onClick = {
                        categoria = if (categoria == c.clave) null else c.clave
                        subcategoria = null
                    },
                    label = { Text("${c.emoji}  ${c.nombre}") },
                )
            }
        }
        val subs = categorias.firstOrNull { it.clave == categoria }?.listaSubcategorias().orEmpty()
        if (subs.isNotEmpty()) {
            Text("Subcategoría", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                subs.forEach { s ->
                    FilterChip(
                        selected = subcategoria == s.nombre,
                        onClick = { subcategoria = if (subcategoria == s.nombre) null else s.nombre },
                        label = { Text(s.nombre) },
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = recordar, onCheckedChange = { recordar = it })
            Text(
                "Usar siempre esta categoría para este comercio",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (gasto?.recurrenteId != null) {
            TarjetaRecurrencia(gasto, recurrente, onGestionarRecurrente)
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("🔁 Repetir cada mes", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Recibos, suscripciones o compras a plazos. Se apuntará solo el mismo día de cada mes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = repetir, onCheckedChange = { repetir = it })
                    }
                    if (repetir) {
                        OutlinedTextField(
                            value = plazosTexto,
                            onValueChange = { plazosTexto = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Número de plazos (vacío = sin fin)") },
                            supportingText = {
                                Text(
                                    if (plazos != null && centimos != null)
                                        "$plazos plazos de ${euros(centimos)} = ${euros(centimos * plazos)} en total. Este es el plazo 1."
                                    else "Sin fin: hasta que lo finalices en ⚙️ → Gastos recurrentes."
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                val base = gasto ?: Gasto(
                    fecha = fecha,
                    importeCentimos = null,
                    comercio = "",
                    origen = Gasto.ORIGEN_MANUAL,
                )
                onGuardar(
                    base.copy(
                        fecha = fecha,
                        importeCentimos = centimos,
                        comercio = comercio.trim(),
                        categoria = categoria,
                        subcategoria = if (categoria == null) null else subcategoria,
                    ),
                    recordar && categoria != null,
                    if (repetir && gasto?.recurrenteId == null) Repeticion(plazos) else null,
                )
            },
            enabled = valido,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (gasto == null) "Añadir gasto" else "Guardar cambios") }

        if (gasto != null) {
            Text(
                "Origen: " + when (gasto.origen) {
                    Gasto.ORIGEN_CARTERA -> "pago con el móvil" + (gasto.tarjeta?.let { " (tarjeta ••$it)" } ?: "")
                    Gasto.ORIGEN_MANUAL -> "añadido a mano"
                    else -> gasto.origen
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (verCalendario) {
        // El DatePicker trabaja en UTC; conservamos la hora original del gasto.
        val local = Instant.ofEpochMilli(fecha).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = local.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { verCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { utc ->
                        val dia = Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
                        fecha = LocalDateTime.of(dia, local.toLocalTime())
                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                    verCalendario = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { verCalendario = false }) { Text("Cancelar") } },
        ) { DatePicker(state = estado) }
    }

    if (mostrarConfirmarBorrado && gasto != null) {
        AlertDialog(
            onDismissRequest = onCerrarConfirmarBorrado,
            title = { Text("¿Borrar este gasto?") },
            text = { Text("${gasto.comercio} · ${gasto.importeCentimos?.let { euros(it) } ?: "sin importe"}. No se puede deshacer.") },
            confirmButton = { TextButton(onClick = { onBorrar(gasto) }) { Text("Borrar") } },
            dismissButton = { TextButton(onClick = onCerrarConfirmarBorrado) { Text("Cancelar") } },
        )
    }
}


/** Repetición elegida al guardar un gasto. plazos = null significa sin fin. */
data class Repeticion(val plazos: Int?)

@Composable
private fun TarjetaRecurrencia(gasto: Gasto, r: Recurrente?, onGestionar: (Recurrente) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (r == null) {
                Text("🔁 Venía de un gasto recurrente que ya se borró.", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            val titulo = if (r.plazos != null) "🔁 Plazo ${gasto.plazo ?: "?"} de ${r.plazos}" else "🔁 Gasto recurrente mensual"
            Text(titulo, style = MaterialTheme.typography.titleSmall)
            val restantes = Recurrencia.plazosRestantes(r)
            if (restantes != null) {
                Text(
                    if (restantes == 0) "Todos los plazos apuntados."
                    else "Quedan $restantes ${if (restantes == 1) "plazo" else "plazos"} · ${euros(r.importeCentimos * restantes)} pendientes",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Recurrencia.siguiente(r)?.let {
                Text("Próximo cargo: ${it.fecha.dayOfMonth}/${it.fecha.monthValue}/${it.fecha.year}", style = MaterialTheme.typography.bodySmall)
            } ?: if (!r.activo && r.plazos == null) Text("Finalizado.", style = MaterialTheme.typography.bodySmall) else Unit
            TextButton(onClick = { onGestionar(r) }) { Text("Cambiar o finalizar la recurrencia") }
        }
    }
}
