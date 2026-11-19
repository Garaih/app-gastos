package com.garaih.appgastos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.garaih.appgastos.datos.ParserCartera

/** Tarjeta blanca con título, usada en Análisis y Ahorro. */
@Composable
fun Seccion(
    titulo: String,
    modifier: Modifier = Modifier,
    accion: (@Composable () -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                accion?.invoke()
            }
            contenido()
        }
    }
}

/** Campo de importe en euros. Devuelve el texto; usar [centimosDe] para convertir. */
@Composable
fun CampoImporte(valor: String, onCambio: (String) -> Unit, etiqueta: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = valor,
        onValueChange = { nuevo -> onCambio(nuevo.filter { it.isDigit() || it == ',' || it == '.' }) },
        label = { Text(etiqueta) },
        suffix = { Text("€") },
        isError = valor.isNotEmpty() && centimosDe(valor) == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}

fun centimosDe(texto: String): Long? = ParserCartera.aCentimos(texto.trim())

/** Fila "etiqueta ...... valor". */
@Composable
fun FilaDato(etiqueta: String, valor: String, destacado: Boolean = false, colorValor: androidx.compose.ui.graphics.Color? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            valor,
            style = if (destacado) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal,
            color = colorValor ?: MaterialTheme.colorScheme.onSurface,
        )
    }
}

fun porcentaje(x: Double): String = "%+.1f %%".format(LOCALE_ES, x * 100)

/** Botón que muestra una fecha y abre un calendario. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SelectorFecha(fecha: Long, onCambio: (Long) -> Unit, modifier: Modifier = Modifier) {
    var abierto by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.material3.OutlinedButton(onClick = { abierto = true }, modifier = modifier.fillMaxWidth()) {
        androidx.compose.material3.Icon(
            androidx.compose.material.icons.Icons.Filled.DateRange, contentDescription = null
        )
        Text("  " + fechaCorta(fecha))
    }
    if (abierto) {
        val zona = java.time.ZoneId.systemDefault()
        val local = java.time.Instant.ofEpochMilli(fecha).atZone(zona).toLocalDateTime()
        val estado = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = local.toLocalDate().atStartOfDay()
                .toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    estado.selectedDateMillis?.let { utc ->
                        val dia = java.time.Instant.ofEpochMilli(utc).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                        onCambio(java.time.LocalDateTime.of(dia, local.toLocalTime()).atZone(zona).toInstant().toEpochMilli())
                    }
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { abierto = false }) { Text("Cancelar") } },
        ) { androidx.compose.material3.DatePicker(state = estado) }
    }
}
