package com.garaih.appgastos.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garaih.appgastos.datos.Categoria
import com.garaih.appgastos.datos.Gasto
import com.garaih.appgastos.datos.Periodo
import com.garaih.appgastos.datos.Ingreso
import com.garaih.appgastos.datos.IngresoFijo
import com.garaih.appgastos.datos.IngresosFijos
import com.garaih.appgastos.datos.Recurrencia
import com.garaih.appgastos.datos.Recurrente
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Un grupo de la pantalla: una categoría, "sin categoría" o "por revisar". */
private data class Grupo(
    val clave: String,
    val nombre: String,
    val emoji: String,
    val color: Color,
    val gastos: List<Gasto>,
    val presupuesto: Long? = null,
) {
    val total: Long = gastos.sumOf { it.importeCentimos ?: 0L }
}

private const val CLAVE_REVISAR = "_revisar"
private const val CLAVE_SIN = "_sin"

@Composable
fun PantallaInicio(
    gastos: List<Gasto>,
    categorias: List<Categoria>,
    presupuestos: Map<String, Long>,
    ingresos: List<Ingreso>,
    ingresosFijos: List<IngresoFijo>,
    recurrentes: List<Recurrente>,
    periodo: Periodo,
    onCambiarPeriodo: (Periodo) -> Unit,
    onAbrirGasto: (Gasto) -> Unit,
    onIrAhorro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hoy = LocalDate.now()
    val periodoActual = Periodo.que(hoy, periodo.diaInicio)
    val esActual = periodo == periodoActual
    val delPeriodo = remember(gastos, periodo) { gastos.filter { periodo.contiene(aFechaLocal(it.fecha)) } }
    val grupos = remember(delPeriodo, categorias, presupuestos) { agrupar(delPeriodo, categorias, presupuestos) }
    val total = delPeriodo.sumOf { it.importeCentimos ?: 0L }
    val totalIngresos = remember(ingresos, ingresosFijos, periodo) {
        ingresos.filter { periodo.contiene(aFechaLocal(it.fecha)) }.sumOf { it.importeCentimos } +
            IngresosFijos.vigentes(ingresosFijos, periodo).sumOf { it.importeCentimos }
    }
    val historial = remember(gastos, periodo) {
        (5 downTo 0).map { n ->
            var p = periodo
            repeat(n) { p = p.anterior() }
            p to gastos.filter { p.contiene(aFechaLocal(it.fecha)) }.sumOf { it.importeCentimos ?: 0L }
        }
    }
    // Cargos recurrentes que aún faltan en este periodo (solo en el periodo en curso).
    val proximos = remember(recurrentes, periodo) {
        if (!esActual) emptyList() else recurrentes.mapNotNull { r ->
            Recurrencia.siguiente(r)?.takeIf { periodo.contiene(it.fecha) && !it.fecha.isBefore(hoy) }?.let { r to it }
        }.sortedBy { it.second.fecha }
    }
    val pendienteRecurrente = proximos.sumOf { it.first.importeCentimos }
    val porClave = categorias.associateBy { it.clave }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SelectorPeriodo(
                periodo = periodo,
                puedeAvanzar = periodo.mes < periodoActual.mes,
                onCambiar = onCambiarPeriodo,
            )
        }
        item {
            TarjetaBalance(
                ingresos = totalIngresos,
                gastos = total,
                pendienteRecurrente = pendienteRecurrente,
                numero = delPeriodo.size,
                diasRestantes = if (esActual) ChronoUnit.DAYS.between(hoy, periodo.finExclusivo) else null,
                totalAnterior = historial.getOrNull(4)?.second ?: 0L,
                presupuestoTotal = presupuestos.values.sum(),
                grupos = grupos.filter { it.clave != CLAVE_REVISAR },
                onIrAhorro = onIrAhorro,
            )
        }
        if (proximos.isNotEmpty()) {
            item {
                Seccion("Próximos cargos del periodo", accion = {
                    Text(euros(pendienteRecurrente), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }) {
                    proximos.take(5).forEach { (r, c) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(34.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(r.categoria?.let { porClave[it]?.emoji } ?: "🔁", fontSize = 16.sp)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(r.comercio, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "Día ${c.fecha.dayOfMonth}" + (r.plazos?.let { " · plazo ${c.plazo} de $it" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(euros(r.importeCentimos), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        if (delPeriodo.isEmpty()) {
            item { EstadoVacio() }
        } else {
            item {
                Seccion("Últimos movimientos") {
                    Column {
                        delPeriodo.sortedByDescending { it.fecha }.take(4).forEach { g ->
                            FilaMovimiento(g, porClave[g.categoria], onClick = { onAbrirGasto(g) })
                        }
                    }
                }
            }
            item { TituloSeccion("Por categoría") }
            items(grupos, key = { it.clave }) { g -> TarjetaGrupo(g, total, onAbrirGasto) }
        }
        item { TarjetaHistorial(historial, periodo, onCambiarPeriodo) }
    }
}

@Composable
private fun TituloSeccion(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp, start = 4.dp),
    )
}

/** Fila compacta con icono de categoría, para "Últimos movimientos". */
@Composable
private fun FilaMovimiento(g: Gasto, cat: Categoria?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = cat?.let { Color(it.color) } ?: Color(0xFF94A3B8)
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) { Text(cat?.emoji ?: if (g.importeCentimos == null) "⚠️" else "❔", fontSize = 18.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(g.comercio, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(fechaHora(g.fecha), g.subcategoria ?: cat?.nombre, if (g.recurrenteId != null) "🔁" else null)
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            g.importeCentimos?.let { "−" + euros(it) } ?: "¿?",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun agrupar(gastos: List<Gasto>, categorias: List<Categoria>, presupuestos: Map<String, Long>): List<Grupo> {
    val porClave = categorias.associateBy { it.clave }
    val revisar = gastos.filter { !it.interpretado || it.importeCentimos == null }
    val resto = gastos - revisar.toSet()
    val grupos = resto.groupBy { g -> g.categoria?.takeIf { it in porClave } }.map { (clave, lista) ->
        val cat = clave?.let { porClave[it] }
        if (cat == null) Grupo(CLAVE_SIN, "Sin categoría", "❔", Color(0xFF94A3B8), lista)
        else Grupo(cat.clave, cat.nombre, cat.emoji, Color(cat.color), lista, presupuestos[cat.clave])
    }
    // Categorías con presupuesto pero sin gastos también se muestran, para ver lo disponible.
    val vacias = categorias.filter { c -> c.clave in presupuestos && grupos.none { it.clave == c.clave } }
        .map { Grupo(it.clave, it.nombre, it.emoji, Color(it.color), emptyList(), presupuestos[it.clave]) }
    val ordenados = (grupos + vacias).sortedByDescending { it.total }
    val cabecera = if (revisar.isNotEmpty())
        listOf(Grupo(CLAVE_REVISAR, "Por revisar", "⚠️", Color(0xFFB45309), revisar)) else emptyList()
    return cabecera + ordenados
}

@Composable
fun SelectorPeriodo(periodo: Periodo, puedeAvanzar: Boolean, onCambiar: (Periodo) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = { onCambiar(periodo.anterior()) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Periodo anterior")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(nombrePeriodo(periodo), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (periodo.diaInicio > 1) {
                Text(
                    "Periodo de ${nombreMesCorto(periodo.mes)} · del día ${periodo.diaInicio}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = { onCambiar(periodo.siguiente()) }, enabled = puedeAvanzar) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Periodo siguiente")
        }
    }
}

@Composable
private fun TarjetaBalance(
    ingresos: Long,
    gastos: Long,
    pendienteRecurrente: Long,
    numero: Int,
    diasRestantes: Long?,
    totalAnterior: Long,
    presupuestoTotal: Long,
    grupos: List<Grupo>,
    onIrAhorro: () -> Unit,
) {
    val primario = MaterialTheme.colorScheme.primary
    val fondo = Brush.linearGradient(listOf(primario, lerp(primario, Color(0xFF0B3B5C), 0.55f)))
    val blanco = Color.White
    val tenue = Color.White.copy(alpha = 0.75f)
    Box(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(fondo).padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (ingresos > 0) {
                val disponible = ingresos - gastos
                Text(if (disponible >= 0) "Te queda este periodo" else "Te has pasado", color = tenue, style = MaterialTheme.typography.bodyMedium)
                Text(euros(kotlin.math.abs(disponible)), color = blanco, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                if (diasRestantes != null && diasRestantes > 0 && disponible > 0) {
                    val libre = (disponible - pendienteRecurrente).coerceAtLeast(0)
                    Text(
                        "≈ ${euros(libre / diasRestantes)} al día durante ${if (diasRestantes == 1L) "1 día" else "$diasRestantes días"}" +
                            if (pendienteRecurrente > 0) " (descontando ${euros(pendienteRecurrente)} de cargos fijos)" else "",
                        color = tenue,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(12.dp))
                // Gastado frente a ingresos
                val fraccion = (gastos.toFloat() / ingresos).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Color.White.copy(alpha = 0.22f))) {
                    Box(
                        Modifier.fillMaxWidth(fraccion).height(10.dp).clip(RoundedCornerShape(5.dp))
                            .background(if (gastos > ingresos) Color(0xFFFCA5A5) else blanco)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth()) {
                    Dato("Ingresos", euros(ingresos), Modifier.weight(1f))
                    Dato("Gastos", euros(gastos), Modifier.weight(1f))
                    Dato("Ahorro", "${((ingresos - gastos) * 100 / ingresos).coerceAtLeast(-999)} %", Modifier.weight(1f))
                }
            } else {
                Text("Gastado este periodo", color = tenue, style = MaterialTheme.typography.bodyMedium)
                Text(euros(gastos), color = blanco, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.18f))
                        .clickable(onClick = onIrAhorro).padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Añade tu nómina para ver el balance  →", color = blanco, style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(8.dp))
            val partes = buildList {
                add("$numero ${if (numero == 1) "gasto" else "gastos"}")
                if (totalAnterior > 0) {
                    val dif = gastos - totalAnterior
                    add("${if (dif >= 0) "+" else "−"}${euros(kotlin.math.abs(dif))} vs anterior")
                }
                if (presupuestoTotal > 0) {
                    val queda = presupuestoTotal - gastos
                    add(if (queda >= 0) "presupuesto: quedan ${euros(queda)}" else "presupuesto superado")
                }
            }
            Text(partes.joinToString(" · "), color = tenue, style = MaterialTheme.typography.bodySmall)
            if (gastos > 0 && grupos.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))) {
                    grupos.filter { it.total > 0 }.forEach { g ->
                        Box(Modifier.weight(g.total.toFloat() / gastos).height(8.dp).background(g.color))
                    }
                }
            }
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(etiqueta, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelMedium)
        Text(valor, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TarjetaHistorial(
    historial: List<Pair<Periodo, Long>>,
    seleccionado: Periodo,
    onElegir: (Periodo) -> Unit,
) {
    val maximo = historial.maxOf { it.second }.coerceAtLeast(1L)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Últimos 6 periodos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                historial.forEach { (p, t) ->
                    val activo = p == seleccionado
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clickable { onElegir(p) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Text(
                            if (t > 0) eurosSinDecimales(t) else "",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height((70.0 * t / maximo).dp.coerceAtLeast(3.dp))
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (activo) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            nombrePeriodoCorto(p),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaGrupo(g: Grupo, totalPeriodo: Long, onAbrirGasto: (Gasto) -> Unit) {
    var abierto by rememberSaveable(g.clave) { mutableStateOf(g.clave == CLAVE_REVISAR) }
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { abierto = !abierto }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(g.color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Text(g.emoji, fontSize = 20.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(g.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                val pct = if (totalPeriodo > 0) (g.total * 100 / totalPeriodo).toInt() else 0
                val limite = g.presupuesto
                val excedido = limite != null && g.total > limite
                Text(
                    when {
                        g.clave == CLAVE_REVISAR -> "${g.gastos.size} · toca para corregir"
                        limite != null -> if (excedido) "Superado en ${euros(g.total - limite)} (límite ${euros(limite)})"
                            else "Quedan ${euros(limite - g.total)} de ${euros(limite)}"
                        else -> "${g.gastos.size} ${if (g.gastos.size == 1) "gasto" else "gastos"} · $pct %"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (excedido) ROJO else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val fraccion = when {
                    g.clave == CLAVE_REVISAR -> null
                    limite != null -> g.total.toFloat() / limite
                    totalPeriodo > 0 -> g.total.toFloat() / totalPeriodo
                    else -> null
                }
                if (fraccion != null) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier.fillMaxWidth().height(if (limite != null) 6.dp else 4.dp).clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            Modifier.fillMaxWidth(fraccion.coerceIn(0f, 1f)).height(if (limite != null) 6.dp else 4.dp)
                                .background(if (excedido) ROJO else g.color)
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                if (g.clave == CLAVE_REVISAR) "" else euros(g.total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        AnimatedVisibility(visible = abierto) {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val porSub = g.gastos.filter { it.subcategoria != null }
                    .groupBy { it.subcategoria!! }
                    .mapValues { (_, l) -> l.sumOf { it.importeCentimos ?: 0L } }
                    .entries.sortedByDescending { it.value }
                if (porSub.isNotEmpty()) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        porSub.forEach { (sub, t) ->
                            AssistChip(onClick = {}, label = { Text("$sub · ${euros(t)}") })
                        }
                    }
                }
                g.gastos.sortedByDescending { it.fecha }.forEach { gasto ->
                    FilaGasto(gasto, onClick = { onAbrirGasto(gasto) })
                }
            }
        }
    }
}

@Composable
fun FilaGasto(g: Gasto, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(g.comercio, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(
                    fechaHora(g.fecha),
                    if (g.recurrenteId != null) ("🔁" + (g.plazo?.let { " plazo $it" } ?: "")) else null,
                    if (g.origen == Gasto.ORIGEN_SANTANDER) "Santander" else null,
                    if (g.origen == Gasto.ORIGEN_CORREO) "📧 correo" else null,
                    g.subcategoria,
                    g.tarjeta?.let { "••$it" },
                    if (g.origen == Gasto.ORIGEN_MANUAL) "manual" else null,
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (g.importeCentimos == null) {
            Icon(Icons.Filled.Warning, contentDescription = "Sin importe", tint = MaterialTheme.colorScheme.tertiary)
        } else {
            Text(euros(g.importeCentimos), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun EstadoVacio() {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🧾", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text("Sin gastos en este periodo", style = MaterialTheme.typography.titleMedium)
        Text(
            "Los pagos con el móvil aparecerán solos. También puedes añadir uno con +.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
    }
}
