package com.garaih.appgastos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.garaih.appgastos.datos.Categoria
import com.garaih.appgastos.datos.Clasificador
import com.garaih.appgastos.datos.Gasto
import com.garaih.appgastos.datos.Periodo
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun PantallaAnalisis(
    gastos: List<Gasto>,
    categorias: List<Categoria>,
    periodo: Periodo,
    onCambiarPeriodo: (Periodo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hoy = LocalDate.now()
    val actual = Periodo.que(hoy, periodo.diaInicio)
    val validos = remember(gastos) { gastos.filter { it.importeCentimos != null } }
    val del = remember(validos, periodo) { validos.filter { periodo.contiene(aFechaLocal(it.fecha)) } }
    val anterior = periodo.anterior()
    val delAnterior = remember(validos, periodo) { validos.filter { anterior.contiene(aFechaLocal(it.fecha)) } }
    val total = del.sumOf { it.importeCentimos!! }
    val porClave = categorias.associateBy { it.clave }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SelectorPeriodo(periodo, periodo.mes < actual.mes, onCambiarPeriodo) }

        // Reparto por categoría
        item {
            val partes = del.groupBy { it.categoria?.takeIf { c -> c in porClave } }
                .map { (clave, l) -> clave to l.sumOf { it.importeCentimos!! } }
                .sortedByDescending { it.second }
            Seccion("Reparto por categoría") {
                if (partes.isEmpty()) {
                    Text("Sin gastos en este periodo.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Rosquilla(
                            porciones = partes.map { (c, v) -> Porcion(v, colorDe(c, porClave)) },
                            centroArriba = euros(total),
                            centroAbajo = "${del.size} gastos",
                        )
                    }
                    partes.forEach { (c, v) ->
                        val cat = c?.let { porClave[it] }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(colorDe(c, porClave)))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                (cat?.let { "${it.emoji} ${it.nombre}" } ?: "❔ Sin categoría"),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${(v * 100 / total.coerceAtLeast(1))} %",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(euros(v), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Ritmo de gasto: acumulado diario frente al periodo anterior
        item {
            val dias = ChronoUnit.DAYS.between(periodo.inicio, periodo.finExclusivo).toInt()
            val transcurridos = if (periodo == actual) ChronoUnit.DAYS.between(periodo.inicio, hoy).toInt() + 1 else dias
            val acumulado = acumulados(del, periodo.inicio, dias).mapIndexed { i, v -> if (i < transcurridos) v else null }
            val diasAnt = ChronoUnit.DAYS.between(anterior.inicio, anterior.finExclusivo).toInt()
            val acumAnt = acumulados(delAnterior, anterior.inicio, diasAnt)
            val primario = MaterialTheme.colorScheme.primary
            val gris = MaterialTheme.colorScheme.outline
            Seccion("Ritmo de gasto") {
                Lineas(listOf(Serie(acumAnt, gris, discontinua = true), Serie(acumulado, primario)))
                Row {
                    Leyenda(primario, "Este periodo")
                    Spacer(Modifier.width(16.dp))
                    Leyenda(gris, "Periodo anterior")
                }
                val media = if (transcurridos > 0) total / transcurridos else 0L
                FilaDato("Media diaria", euros(media))
                if (periodo == actual) {
                    FilaDato("Proyección a fin de periodo", euros(media * dias), destacado = true)
                    val antHastaHoy = acumAnt.getOrNull(transcurridos - 1) ?: 0L
                    if (antHastaHoy > 0) {
                        val dif = total - antHastaHoy
                        FilaDato(
                            "Frente al anterior a estas alturas",
                            (if (dif >= 0) "+" else "−") + euros(kotlin.math.abs(dif)),
                            colorValor = if (dif > 0) ROJO else VERDE,
                        )
                    }
                }
            }
        }

        // Evolución de 12 periodos
        item {
            val historial = remember(validos, periodo) {
                (11 downTo 0).map { n ->
                    var p = periodo
                    repeat(n) { p = p.anterior() }
                    p to validos.filter { p.contiene(aFechaLocal(it.fecha)) }.sumOf { it.importeCentimos!! }
                }
            }
            val conDatos = historial.filter { it.second > 0 }
            Seccion("Evolución (12 periodos)") {
                Lineas(listOf(Serie(historial.map { it.second }, MaterialTheme.colorScheme.primary)), alto = 140.dp)
                Row(Modifier.fillMaxWidth()) {
                    Text(nombrePeriodoCorto(historial.first().first), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    Text(nombrePeriodoCorto(historial.last().first), style = MaterialTheme.typography.labelSmall)
                }
                if (conDatos.isNotEmpty()) {
                    FilaDato("Media por periodo", euros(conDatos.sumOf { it.second } / conDatos.size))
                    val max = conDatos.maxBy { it.second }
                    FilaDato("Periodo más caro", "${nombrePeriodoCorto(max.first)} · ${euros(max.second)}")
                }
            }
        }

        // Dónde más gastas
        item {
            val top = del.groupBy { Clasificador.normalizar(it.comercio) }
                .map { (_, l) -> Triple(l.first().comercio, l.size, l.sumOf { it.importeCentimos!! }) }
                .sortedByDescending { it.third }
                .take(5)
            Seccion("Dónde más gastas") {
                if (top.isEmpty()) Text("Sin datos.", style = MaterialTheme.typography.bodyMedium)
                top.forEachIndexed { i, (nombre, veces, suma) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(24.dp))
                        Column(Modifier.weight(1f)) {
                            Text(nombre, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "$veces ${if (veces == 1) "vez" else "veces"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(euros(suma), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
                del.maxByOrNull { it.importeCentimos!! }?.let {
                    FilaDato("Mayor gasto", "${it.comercio} · ${euros(it.importeCentimos!!)}")
                }
            }
        }
    }
}

val VERDE = Color(0xFF16A34A)
val ROJO = Color(0xFFDC2626)

private val GRIS_SIN = Color(0xFF94A3B8)

private fun colorDe(clave: String?, porClave: Map<String, Categoria>): Color =
    clave?.let { porClave[it] }?.let { Color(it.color) } ?: GRIS_SIN

/** Gasto acumulado al final de cada día del periodo. */
private fun acumulados(gastos: List<Gasto>, inicio: LocalDate, dias: Int): List<Long?> {
    val porDia = LongArray(dias)
    gastos.forEach { g ->
        val d = ChronoUnit.DAYS.between(inicio, aFechaLocal(g.fecha)).toInt()
        if (d in 0 until dias) porDia[d] += g.importeCentimos ?: 0L
    }
    var suma = 0L
    return porDia.map { suma += it; suma }
}

@Composable
private fun Leyenda(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.bodySmall)
    }
}
