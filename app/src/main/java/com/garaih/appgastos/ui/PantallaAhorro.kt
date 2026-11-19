package com.garaih.appgastos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garaih.appgastos.datos.Gasto
import com.garaih.appgastos.datos.Ingreso
import com.garaih.appgastos.datos.IngresoFijo
import com.garaih.appgastos.datos.IngresosFijos
import com.garaih.appgastos.datos.Inversion
import com.garaih.appgastos.datos.Objetivo
import com.garaih.appgastos.datos.Periodo
import com.garaih.appgastos.datos.ganancia
import com.garaih.appgastos.datos.rentabilidad
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val COLORES_TIPO = listOf(
    0xFF0F766E, 0xFF3B82F6, 0xFFA855F7, 0xFFF59E0B, 0xFF22A55E, 0xFFEF4444, 0xFF64748B,
)

/** Acciones de la pantalla de ahorro (las ejecuta AppGastos con el repositorio). */
class AccionesAhorro(
    /** (concepto anterior o null si es nuevo, concepto, importe en céntimos, periodo desde el que aplica) */
    val cambiarIngresoFijo: (String?, String, Long, Periodo) -> Unit,
    val guardarIngreso: (Ingreso) -> Unit,
    val borrarIngreso: (Ingreso) -> Unit,
    val guardarObjetivo: (Objetivo) -> Unit,
    val borrarObjetivo: (Objetivo) -> Unit,
    val guardarInversion: (Inversion) -> Unit,
    val borrarInversion: (Inversion) -> Unit,
)

private sealed interface Dialogo {
    data class EditFijo(val fijo: IngresoFijo?) : Dialogo
    data class EditIngreso(val ingreso: Ingreso?) : Dialogo
    data class EditObjetivo(val objetivo: Objetivo?) : Dialogo
    data class Aportar(val objetivo: Objetivo) : Dialogo
    data class EditInversion(val inversion: Inversion?) : Dialogo
}

@Composable
fun PantallaAhorro(
    gastos: List<Gasto>,
    ingresos: List<Ingreso>,
    ingresosFijos: List<IngresoFijo>,
    objetivos: List<Objetivo>,
    inversiones: List<Inversion>,
    periodo: Periodo,
    onCambiarPeriodo: (Periodo) -> Unit,
    acciones: AccionesAhorro,
    modifier: Modifier = Modifier,
) {
    var dialogo by remember { mutableStateOf<Dialogo?>(null) }
    val actual = Periodo.que(LocalDate.now(), periodo.diaInicio)
    val ingresosPeriodo = ingresos.filter { periodo.contiene(aFechaLocal(it.fecha)) }
    val fijos = IngresosFijos.vigentes(ingresosFijos, periodo)
    val totalIngresos = ingresosPeriodo.sumOf { it.importeCentimos } + fijos.sumOf { it.importeCentimos }
    val totalGastos = gastos.filter { periodo.contiene(aFechaLocal(it.fecha)) }.sumOf { it.importeCentimos ?: 0L }
    val ahorro = totalIngresos - totalGastos

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SelectorPeriodo(periodo, periodo.mes < actual.mes, onCambiarPeriodo) }

        // Balance del periodo
        item {
            Seccion(
                "Balance del periodo",
                accion = {
                    Row {
                        TextButton(onClick = { dialogo = Dialogo.EditFijo(null) }) { Text("+ Fijo") }
                        TextButton(onClick = { dialogo = Dialogo.EditIngreso(null) }) { Text("+ Puntual") }
                    }
                },
            ) {
                FilaDato("Ingresos", euros(totalIngresos), colorValor = VERDE)
                FilaDato("Gastos", "−" + euros(totalGastos), colorValor = ROJO)
                FilaDato(
                    if (ahorro >= 0) "Ahorrado" else "Déficit",
                    euros(kotlin.math.abs(ahorro)),
                    destacado = true,
                    colorValor = if (ahorro >= 0) VERDE else ROJO,
                )
                if (totalIngresos > 0) {
                    val tasa = ahorro.toDouble() / totalIngresos
                    Text(
                        "Tasa de ahorro: ${"%.0f".format(tasa * 100)} %",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    BarraProgreso((tasa).coerceIn(0.0, 1.0).toFloat(), VERDE)
                } else {
                    Text(
                        "Añade tu nómina como ingreso fijo (se repite cada periodo) y los extras como puntuales para ver cuánto ahorras.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                fijos.forEach { f ->
                    Row(
                        Modifier.fillMaxWidth().clickable { dialogo = Dialogo.EditFijo(f) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("🔁 ${f.concepto}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Fijo cada periodo · toca para cambiarlo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("+" + euros(f.importeCentimos), color = VERDE)
                    }
                }
                ingresosPeriodo.forEach { i ->
                    Row(
                        Modifier.fillMaxWidth().clickable { dialogo = Dialogo.EditIngreso(i) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(i.concepto, style = MaterialTheme.typography.bodyMedium)
                            Text(fechaCorta(i.fecha), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("+" + euros(i.importeCentimos), color = VERDE)
                    }
                }
            }
        }

        // Objetivos de ahorro
        item {
            Seccion(
                "Objetivos de ahorro",
                accion = {
                    IconButton(onClick = { dialogo = Dialogo.EditObjetivo(null) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Nuevo objetivo")
                    }
                },
            ) {
                if (objetivos.isEmpty()) {
                    Text(
                        "Crea un objetivo (viaje, colchón de emergencia, coche...) y ve apartando dinero.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                objetivos.forEach { o -> FilaObjetivo(o, onEditar = { dialogo = Dialogo.EditObjetivo(o) }, onAportar = { dialogo = Dialogo.Aportar(o) }) }
            }
        }

        // Inversiones
        item {
            val invertido = inversiones.sumOf { it.invertidoCentimos }
            val valor = inversiones.sumOf { it.valorCentimos }
            val ganancia = valor - invertido
            Seccion(
                "Inversiones",
                accion = {
                    IconButton(onClick = { dialogo = Dialogo.EditInversion(null) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Nueva inversión")
                    }
                },
            ) {
                if (inversiones.isEmpty()) {
                    Text(
                        "Apunta tus inversiones y actualiza su valor de vez en cuando para ver la rentabilidad. " +
                            "Los valores los introduces tú: la app no se conecta a internet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val porTipo = inversiones.groupBy { it.tipo }.map { (t, l) -> t to l.sumOf { it.valorCentimos } }
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Rosquilla(
                            porciones = porTipo.map { (t, v) -> Porcion(v, colorTipo(t)) },
                            centroArriba = euros(valor),
                            centroAbajo = porcentaje(if (invertido > 0) ganancia.toDouble() / invertido else 0.0),
                            tamano = 160.dp,
                        )
                    }
                    FilaDato("Invertido", euros(invertido))
                    FilaDato("Valor actual", euros(valor), destacado = true)
                    FilaDato(
                        "Ganancia",
                        (if (ganancia >= 0) "+" else "−") + euros(kotlin.math.abs(ganancia)),
                        colorValor = if (ganancia >= 0) VERDE else ROJO,
                    )
                    inversiones.forEach { inv ->
                        Row(
                            Modifier.fillMaxWidth().clickable { dialogo = Dialogo.EditInversion(inv) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(colorTipo(inv.tipo)))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(inv.nombre, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${inv.tipo} · actualizado ${fechaCorta(inv.actualizado)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(euros(inv.valorCentimos), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    porcentaje(inv.rentabilidad()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (inv.ganancia() >= 0) VERDE else ROJO,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    when (val d = dialogo) {
        is Dialogo.EditFijo -> DialogoIngresoFijo(d.fijo, periodo, onCerrar = { dialogo = null }, acciones)
        is Dialogo.EditIngreso -> DialogoIngreso(d.ingreso, onCerrar = { dialogo = null }, acciones)
        is Dialogo.EditObjetivo -> DialogoObjetivo(d.objetivo, onCerrar = { dialogo = null }, acciones)
        is Dialogo.Aportar -> DialogoAportar(d.objetivo, onCerrar = { dialogo = null }, acciones)
        is Dialogo.EditInversion -> DialogoInversion(d.inversion, onCerrar = { dialogo = null }, acciones)
        null -> Unit
    }
}

private fun colorTipo(tipo: String): Color {
    val i = Inversion.TIPOS.indexOf(tipo).let { if (it < 0) COLORES_TIPO.lastIndex else it }
    return Color(COLORES_TIPO[i % COLORES_TIPO.size])
}

@Composable
private fun BarraProgreso(fraccion: Float, color: Color) {
    Box(
        Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(Modifier.fillMaxWidth(fraccion.coerceIn(0f, 1f)).height(8.dp).background(color))
    }
}

@Composable
private fun FilaObjetivo(o: Objetivo, onEditar: () -> Unit, onAportar: () -> Unit) {
    val fraccion = if (o.objetivoCentimos > 0) o.ahorradoCentimos.toFloat() / o.objetivoCentimos else 0f
    val falta = (o.objetivoCentimos - o.ahorradoCentimos).coerceAtLeast(0)
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onEditar).padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(o.emoji, fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(o.nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "${euros(o.ahorradoCentimos)} de ${euros(o.objetivoCentimos)} · ${(fraccion * 100).toInt()} %",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onAportar) { Text("Aportar") }
        }
        BarraProgreso(fraccion, if (falta == 0L) VERDE else MaterialTheme.colorScheme.primary)
        val texto = when {
            falta == 0L -> "🎉 ¡Objetivo conseguido!"
            o.fechaLimite != null -> {
                val hoy = LocalDate.now()
                val limite = java.time.Instant.ofEpochMilli(o.fechaLimite).atZone(ZoneId.systemDefault()).toLocalDate()
                val meses = ChronoUnit.MONTHS.between(hoy.withDayOfMonth(1), limite.withDayOfMonth(1)).coerceAtLeast(1)
                if (limite.isBefore(hoy)) "Plazo vencido (${fechaCorta(o.fechaLimite)}) · faltan ${euros(falta)}"
                else "Faltan ${euros(falta)} · aparta ${euros(falta / meses)} al mes hasta ${fechaCorta(o.fechaLimite)}"
            }
            else -> "Faltan ${euros(falta)}"
        }
        Text(texto, style = MaterialTheme.typography.bodySmall)
    }
}

// ---------- Diálogos ----------

@Composable
private fun DialogoIngresoFijo(fijo: IngresoFijo?, periodo: Periodo, onCerrar: () -> Unit, acciones: AccionesAhorro) {
    var concepto by remember { mutableStateOf(fijo?.concepto ?: "Nómina") }
    var importe by remember { mutableStateOf(importeEditable(fijo?.importeCentimos)) }
    var confirmarQuitar by remember { mutableStateOf(false) }
    val c = centimosDe(importe)
    val desde = nombrePeriodo(periodo)
    if (confirmarQuitar && fijo != null) {
        AlertDialog(
            onDismissRequest = onCerrar,
            title = { Text("¿Dejar de contar \"${fijo.concepto}\"?") },
            text = { Text("Desde el periodo $desde no se sumará. Los periodos anteriores no cambian.") },
            confirmButton = {
                TextButton(onClick = { acciones.cambiarIngresoFijo(fijo.concepto, fijo.concepto, 0, periodo); onCerrar() }) {
                    Text("Dejar de contar", color = ROJO)
                }
            },
            dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
        )
        return
    }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (fijo == null) "Nuevo ingreso fijo" else "Cambiar ingreso fijo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(concepto, { concepto = it }, label = { Text("Concepto") }, singleLine = true)
                CampoImporte(importe, { importe = it }, "Importe por periodo")
                Text(
                    "Se aplica desde este periodo ($desde) y queda como importe por defecto para los siguientes. " +
                        "Los periodos anteriores conservan su importe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = c != null && c > 0 && concepto.isNotBlank(),
                onClick = { acciones.cambiarIngresoFijo(fijo?.concepto, concepto.trim(), c!!, periodo); onCerrar() },
            ) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (fijo != null) TextButton(onClick = { confirmarQuitar = true }) { Text("Quitar", color = ROJO) }
                TextButton(onClick = onCerrar) { Text("Cancelar") }
            }
        },
    )
}

@Composable
private fun DialogoIngreso(ingreso: Ingreso?, onCerrar: () -> Unit, acciones: AccionesAhorro) {
    var concepto by remember { mutableStateOf(ingreso?.concepto ?: "") }
    var importe by remember { mutableStateOf(importeEditable(ingreso?.importeCentimos)) }
    var fecha by remember { mutableStateOf(ingreso?.fecha ?: System.currentTimeMillis()) }
    val c = centimosDe(importe)
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (ingreso == null) "Nuevo ingreso puntual" else "Editar ingreso puntual") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(concepto, { concepto = it }, label = { Text("Concepto") }, singleLine = true)
                CampoImporte(importe, { importe = it }, "Importe")
                SelectorFecha(fecha, { fecha = it })
            }
        },
        confirmButton = {
            TextButton(
                enabled = c != null && c > 0 && concepto.isNotBlank(),
                onClick = {
                    acciones.guardarIngreso(
                        (ingreso ?: Ingreso(fecha = fecha, importeCentimos = 0, concepto = ""))
                            .copy(fecha = fecha, importeCentimos = c!!, concepto = concepto.trim())
                    )
                    onCerrar()
                },
            ) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (ingreso != null) TextButton(onClick = { acciones.borrarIngreso(ingreso); onCerrar() }) { Text("Borrar", color = ROJO) }
                TextButton(onClick = onCerrar) { Text("Cancelar") }
            }
        },
    )
}

@Composable
private fun DialogoObjetivo(objetivo: Objetivo?, onCerrar: () -> Unit, acciones: AccionesAhorro) {
    var nombre by remember { mutableStateOf(objetivo?.nombre ?: "") }
    var emoji by remember { mutableStateOf(objetivo?.emoji ?: "🎯") }
    var importe by remember { mutableStateOf(importeEditable(objetivo?.objetivoCentimos)) }
    var conFecha by remember { mutableStateOf(objetivo?.fechaLimite != null) }
    var fecha by remember {
        mutableStateOf(objetivo?.fechaLimite ?: LocalDate.now().plusMonths(12).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
    }
    val c = centimosDe(importe)
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (objetivo == null) "Nuevo objetivo" else "Editar objetivo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(emoji, { emoji = it.take(4) }, label = { Text("Icono") }, singleLine = true, modifier = Modifier.width(80.dp))
                    OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                CampoImporte(importe, { importe = it }, "Cantidad objetivo")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Checkbox(checked = conFecha, onCheckedChange = { conFecha = it })
                    Text("Con fecha límite")
                }
                if (conFecha) SelectorFecha(fecha, { fecha = it })
            }
        },
        confirmButton = {
            TextButton(
                enabled = c != null && c > 0 && nombre.isNotBlank(),
                onClick = {
                    acciones.guardarObjetivo(
                        (objetivo ?: Objetivo(nombre = "", emoji = "", objetivoCentimos = 0)).copy(
                            nombre = nombre.trim(),
                            emoji = emoji.trim().ifEmpty { "🎯" },
                            objetivoCentimos = c!!,
                            fechaLimite = if (conFecha) fecha else null,
                        )
                    )
                    onCerrar()
                },
            ) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (objetivo != null) TextButton(onClick = { acciones.borrarObjetivo(objetivo); onCerrar() }) { Text("Borrar", color = ROJO) }
                TextButton(onClick = onCerrar) { Text("Cancelar") }
            }
        },
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun DialogoAportar(objetivo: Objetivo, onCerrar: () -> Unit, acciones: AccionesAhorro) {
    var importe by remember { mutableStateOf("") }
    var retirar by remember { mutableStateOf(false) }
    val c = centimosDe(importe)
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("${objetivo.emoji} ${objetivo.nombre}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !retirar, onClick = { retirar = false },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text("Aportar") }
                    SegmentedButton(
                        selected = retirar, onClick = { retirar = true },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text("Retirar") }
                }
                CampoImporte(importe, { importe = it }, "Importe")
                Text("Llevas ${euros(objetivo.ahorradoCentimos)} de ${euros(objetivo.objetivoCentimos)}.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                enabled = c != null && c > 0,
                onClick = {
                    val delta = if (retirar) -c!! else c!!
                    acciones.guardarObjetivo(objetivo.copy(ahorradoCentimos = (objetivo.ahorradoCentimos + delta).coerceAtLeast(0)))
                    onCerrar()
                },
            ) { Text("Confirmar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoInversion(inversion: Inversion?, onCerrar: () -> Unit, acciones: AccionesAhorro) {
    var nombre by remember { mutableStateOf(inversion?.nombre ?: "") }
    var tipo by remember { mutableStateOf(inversion?.tipo ?: Inversion.TIPOS.first()) }
    var invertido by remember { mutableStateOf(importeEditable(inversion?.invertidoCentimos)) }
    var valor by remember { mutableStateOf(importeEditable(inversion?.valorCentimos)) }
    val ci = centimosDe(invertido)
    val cv = centimosDe(valor.ifBlank { invertido })
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (inversion == null) "Nueva inversión" else "Editar inversión") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre (fondo, broker...)") }, singleLine = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Inversion.TIPOS.forEach { t ->
                        FilterChip(selected = tipo == t, onClick = { tipo = t }, label = { Text(t) })
                    }
                }
                CampoImporte(invertido, { invertido = it }, "Total aportado")
                CampoImporte(valor, { valor = it }, "Valor actual")
                Text(
                    "Actualiza el valor cuando quieras para seguir la rentabilidad.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = ci != null && cv != null && nombre.isNotBlank(),
                onClick = {
                    acciones.guardarInversion(
                        (inversion ?: Inversion(nombre = "", tipo = tipo, invertidoCentimos = 0, valorCentimos = 0)).copy(
                            nombre = nombre.trim(),
                            tipo = tipo,
                            invertidoCentimos = ci!!,
                            valorCentimos = cv!!,
                            actualizado = System.currentTimeMillis(),
                        )
                    )
                    onCerrar()
                },
            ) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (inversion != null) TextButton(onClick = { acciones.borrarInversion(inversion); onCerrar() }) { Text("Borrar", color = ROJO) }
                TextButton(onClick = onCerrar) { Text("Cancelar") }
            }
        },
    )
}
