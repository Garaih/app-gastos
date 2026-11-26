package com.garaih.appgastos.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.garaih.appgastos.datos.Categoria
import com.garaih.appgastos.datos.Gasto
import com.garaih.appgastos.datos.Recurrencia
import com.garaih.appgastos.datos.Recurrente
import com.garaih.appgastos.datos.Suscripciones
import com.garaih.appgastos.recordatorios.Recordatorios

/** Fijos y suscripciones: lo que pagas cada mes, compras a plazos y suscripciones detectadas. */
@Composable
fun PantallaRecurrentes(
    recurrentes: List<Recurrente>,
    gastos: List<Gasto>,
    categorias: List<Categoria>,
    onEditar: (Recurrente) -> Unit,
    onAceptarSugerencia: (Suscripciones.Sugerencia) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fijos = recurrentes.filter { it.activo && it.plazos == null }
    val plazos = recurrentes.filter { it.activo && it.plazos != null }
    val terminados = recurrentes.filter { !it.activo }
    val porClave = categorias.associateBy { it.clave }
    var ignoradas by remember { mutableStateOf(setOf<String>()) }
    val sugerencias = remember(gastos, recurrentes, ignoradas) {
        Suscripciones.detectar(gastos, recurrentes).filter { it.comercio !in ignoradas }
    }
    val pagadoPor = remember(gastos) {
        gastos.filter { it.recurrenteId != null }.groupBy { it.recurrenteId!! }
            .mapValues { (_, l) -> l.sumOf { it.importeCentimos ?: 0L } }
    }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Seccion("Lo que pagas fijo") {
                val mensual = fijos.sumOf { it.importeCentimos }
                FilaDato("Suscripciones y recibos al mes", euros(mensual), destacado = true)
                FilaDato("Al año", euros(mensual * 12))
                if (plazos.isNotEmpty()) {
                    FilaDato("Compras a plazos al mes", euros(plazos.sumOf { it.importeCentimos }))
                    val pendiente = plazos.sumOf { r -> (Recurrencia.plazosRestantes(r) ?: 0) * r.importeCentimos }
                    FilaDato("Pendiente de pagar a plazos", euros(pendiente))
                }
                Text(
                    "Toca uno para cambiarlo, ponerle un aviso antes del cargo o marcar que quieres darlo de baja. " +
                        "Para añadir uno: + Añadir gasto → \"Repetir cada mes\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (sugerencias.isNotEmpty()) {
            item { Titulo("💡 Posibles suscripciones") }
            items(sugerencias, key = { "s" + it.comercio }) { s ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(s.comercio, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${euros(s.importeCentimos)} en ${s.meses} meses seguidos, hacia el día ${s.dia}. ¿Es un gasto fijo?",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row {
                            TextButton(onClick = { onAceptarSugerencia(s) }) { Text("Sí, marcar como fijo") }
                            TextButton(onClick = { ignoradas = ignoradas + s.comercio }) { Text("No") }
                        }
                    }
                }
            }
        }

        if (fijos.isNotEmpty()) item { Titulo("Suscripciones y recibos") }
        items(fijos, key = { it.id }) { r -> FilaRecurrente(r, porClave[r.categoria], pagadoPor[r.id] ?: 0, onEditar) }
        if (plazos.isNotEmpty()) item { Titulo("Compras a plazos") }
        items(plazos, key = { it.id }) { r -> FilaRecurrente(r, porClave[r.categoria], pagadoPor[r.id] ?: 0, onEditar) }
        if (terminados.isNotEmpty()) item { Titulo("Terminados o dados de baja") }
        items(terminados, key = { it.id }) { r -> FilaRecurrente(r, porClave[r.categoria], pagadoPor[r.id] ?: 0, onEditar) }
        if (recurrentes.isEmpty() && sugerencias.isEmpty()) {
            item { Text("Aún no tienes gastos fijos.", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
}

@Composable
private fun FilaRecurrente(r: Recurrente, cat: Categoria?, pagado: Long, onEditar: (Recurrente) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEditar(r) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(cat?.emoji ?: "🔁", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    r.comercio + (if (r.quiereCancelar && r.activo) "  ✂️" else "") + (if (r.recordarDias != null && r.activo) "  🔔" else ""),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(detalle(r), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (pagado > 0) {
                    Text("Pagado hasta ahora: ${euros(pagado)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    euros(r.importeCentimos),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (r.activo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (r.plazos == null && r.activo) {
                    Text("${euros(r.importeCentimos * 12)}/año", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        !r.activo -> if (r.quiereCancelar) "dado de baja" else "finalizado"
        sig != null -> "próximo ${sig.fecha.dayOfMonth}/${sig.fecha.monthValue}"
        else -> "terminado"
    }
    return partes.joinToString(" · ")
}

/** Cambiar importe, día, plazos y avisos (afecta a los cargos aún no apuntados), finalizar o borrar. */
@Composable
fun DialogoRecurrente(
    r: Recurrente,
    onGuardar: (Recurrente) -> Unit,
    onFinalizar: (Recurrente) -> Unit,
    onBorrar: (Recurrente) -> Unit,
    onCerrar: () -> Unit,
) {
    val context = LocalContext.current
    var comercio by remember { mutableStateOf(r.comercio) }
    var importe by remember { mutableStateOf(importeEditable(r.importeCentimos)) }
    var dia by remember { mutableStateOf(r.dia.toString()) }
    var plazos by remember { mutableStateOf(r.plazos?.toString() ?: "") }
    var avisar by remember { mutableStateOf(r.recordarDias != null) }
    var diasAviso by remember { mutableStateOf((r.recordarDias ?: 3).toString()) }
    var quiereCancelar by remember { mutableStateOf(r.quiereCancelar) }
    var confirmarBorrar by remember { mutableStateOf(false) }
    var sinPermiso by remember { mutableStateOf(false) }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        sinPermiso = !ok
        if (!ok) avisar = false
    }
    fun activarAviso() {
        avisar = true
        if (!Recordatorios.puedeNotificar(context) && Build.VERSION.SDK_INT >= 33) {
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val c = centimosDe(importe)
    val d = dia.toIntOrNull()?.takeIf { it in 1..28 }
    val minimoPlazos = Recurrencia.numeroPlazo(r, Recurrencia.aMes(r.ultimoMes))
    val p = plazos.toIntOrNull()
    val plazosValidos = plazos.isBlank() || (p != null && p >= minimoPlazos)
    val da = diasAviso.toIntOrNull()?.takeIf { it in 0..20 }

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
        title = { Text("Gasto fijo") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔔 Avisarme antes del cargo", modifier = Modifier.weight(1f))
                    Switch(checked = avisar, onCheckedChange = { if (it) activarAviso() else avisar = false })
                }
                if (avisar) {
                    OutlinedTextField(
                        diasAviso, { diasAviso = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Días de antelación") }, singleLine = true, isError = da == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = quiereCancelar, onCheckedChange = { quiereCancelar = it })
                        Text("✂️ Quiero darla de baja (el aviso me lo recordará)")
                    }
                }
                if (sinPermiso) {
                    Text(
                        "Sin permiso de notificaciones no puedo avisarte. Actívalo en los ajustes de la app.",
                        color = ROJO, style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    "Los cambios afectan a los próximos cargos; los ya apuntados no cambian.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row {
                    if (r.activo) {
                        OutlinedButton(onClick = { onFinalizar(r.copy(quiereCancelar = quiereCancelar)); onCerrar() }) {
                            Text(if (quiereCancelar) "Ya la he dado de baja" else "Finalizar ya")
                        }
                    }
                    TextButton(onClick = { confirmarBorrar = true }) { Text("Borrar", color = ROJO) }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = c != null && c > 0 && d != null && plazosValidos && comercio.isNotBlank() && (!avisar || da != null),
                onClick = {
                    val nuevoAviso = if (avisar) da else null
                    onGuardar(
                        r.copy(
                            comercio = comercio.trim(), importeCentimos = c!!, dia = d!!,
                            plazos = if (plazos.isBlank()) null else p,
                            activo = true,
                            recordarDias = nuevoAviso,
                            quiereCancelar = avisar && quiereCancelar,
                            // Si cambia el aviso, se puede volver a avisar este mes.
                            avisadoMes = if (nuevoAviso != r.recordarDias || quiereCancelar != r.quiereCancelar) null else r.avisadoMes,
                        )
                    )
                    onCerrar()
                },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}
