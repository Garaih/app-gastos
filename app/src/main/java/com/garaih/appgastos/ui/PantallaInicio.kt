package com.garaih.appgastos.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garaih.appgastos.datos.Categoria
import com.garaih.appgastos.datos.Gasto
import java.time.YearMonth

/** Un grupo de la pantalla: una categoría, "sin categoría" o "por revisar". */
private data class Grupo(
    val clave: String,
    val nombre: String,
    val emoji: String,
    val color: Color,
    val gastos: List<Gasto>,
) {
    val total: Long = gastos.sumOf { it.importeCentimos ?: 0L }
}

@Composable
fun PantallaInicio(
    gastos: List<Gasto>,
    mes: YearMonth,
    onCambiarMes: (YearMonth) -> Unit,
    onAbrirGasto: (Gasto) -> Unit,
    modifier: Modifier = Modifier,
) {
    val delMes = remember(gastos, mes) { gastos.filter { YearMonth.from(aFechaLocal(it.fecha)) == mes } }
    val grupos = remember(delMes) { agrupar(delMes) }
    val total = delMes.sumOf { it.importeCentimos ?: 0L }
    val totalMesAnterior = remember(gastos, mes) {
        val anterior = mes.minusMonths(1)
        gastos.filter { YearMonth.from(aFechaLocal(it.fecha)) == anterior }.sumOf { it.importeCentimos ?: 0L }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SelectorMes(mes, onCambiarMes) }
        item {
            TarjetaTotal(
                total = total,
                numero = delMes.size,
                mes = mes,
                totalMesAnterior = totalMesAnterior,
                grupos = grupos.filter { it.clave != CLAVE_REVISAR },
            )
        }
        if (delMes.isEmpty()) {
            item { EstadoVacio() }
        } else {
            item {
                Text(
                    "Por categoría",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                )
            }
            items(grupos, key = { it.clave }) { g ->
                TarjetaGrupo(g, total, onAbrirGasto)
            }
        }
    }
}

private const val CLAVE_REVISAR = "_revisar"
private const val CLAVE_SIN = "_sin"

private fun agrupar(gastos: List<Gasto>): List<Grupo> {
    val revisar = gastos.filter { !it.interpretado || it.importeCentimos == null }
    val resto = gastos - revisar.toSet()
    val porCategoria = resto.groupBy { Categoria.desdeClave(it.categoria) }
    val grupos = porCategoria.map { (cat, lista) ->
        if (cat == null) Grupo(CLAVE_SIN, "Sin categoría", "❔", Color(0xFF94A3B8), lista)
        else Grupo(cat.clave, cat.nombre, cat.emoji, Color(cat.color), lista)
    }.sortedByDescending { it.total }
    val cabecera = if (revisar.isNotEmpty())
        listOf(Grupo(CLAVE_REVISAR, "Por revisar", "⚠️", Color(0xFFB45309), revisar)) else emptyList()
    return cabecera + grupos
}

@Composable
private fun SelectorMes(mes: YearMonth, onCambiarMes: (YearMonth) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = { onCambiarMes(mes.minusMonths(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mes anterior")
        }
        Text(nombreMes(mes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        val esActual = mes >= YearMonth.now()
        IconButton(onClick = { onCambiarMes(mes.plusMonths(1)) }, enabled = !esActual) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mes siguiente")
        }
    }
}

@Composable
private fun TarjetaTotal(
    total: Long,
    numero: Int,
    mes: YearMonth,
    totalMesAnterior: Long,
    grupos: List<Grupo>,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Gastado en ${nombreMesCorto(mes)}", style = MaterialTheme.typography.bodyMedium)
            Text(
                euros(total),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            val comparacion = if (totalMesAnterior > 0) {
                val dif = total - totalMesAnterior
                val signo = if (dif >= 0) "+" else "−"
                " · $signo${euros(kotlin.math.abs(dif))} vs mes anterior"
            } else ""
            Text(
                "$numero ${if (numero == 1) "gasto" else "gastos"}$comparacion",
                style = MaterialTheme.typography.bodySmall,
            )
            if (total > 0 && grupos.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                BarraReparto(grupos, total)
            }
        }
    }
}

@Composable
private fun BarraReparto(grupos: List<Grupo>, total: Long) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color.White.copy(alpha = 0.25f))
    ) {
        grupos.filter { it.total > 0 }.forEach { g ->
            Box(
                Modifier
                    .weight(g.total.toFloat() / total)
                    .height(10.dp)
                    .background(g.color)
            )
        }
    }
}

@Composable
private fun TarjetaGrupo(g: Grupo, totalMes: Long, onAbrirGasto: (Gasto) -> Unit) {
    var abierto by rememberSaveable(g.clave) { mutableStateOf(g.clave == CLAVE_REVISAR) }
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { abierto = !abierto }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(g.color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Text(g.emoji, fontSize = 20.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(g.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                val pct = if (totalMes > 0) (g.total * 100 / totalMes).toInt() else 0
                Text(
                    "${g.gastos.size} ${if (g.gastos.size == 1) "gasto" else "gastos"}" +
                        if (g.clave != CLAVE_REVISAR) " · $pct %" else " · toca para corregir",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (g.clave != CLAVE_REVISAR && totalMes > 0) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            Modifier.fillMaxWidth(g.total.toFloat() / totalMes).height(4.dp)
                                .background(g.color)
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
                g.gastos.sortedByDescending { it.fecha }.forEach { gasto ->
                    FilaGasto(gasto, onClick = { onAbrirGasto(gasto) })
                }
            }
        }
    }
}

@Composable
private fun FilaGasto(g: Gasto, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(g.comercio, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                fechaHora(g.fecha) + (g.tarjeta?.let { " · ••$it" } ?: "") +
                    if (g.origen == Gasto.ORIGEN_MANUAL) " · manual" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🧾", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text("Sin gastos este mes", style = MaterialTheme.typography.titleMedium)
        Text(
            "Los pagos con el móvil aparecerán solos. También puedes añadir uno con +.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
    }
}
