package com.garaih.appgastos.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Porcion(val valor: Long, val color: Color)

/** Gráfico de rosquilla con un texto en el centro. */
@Composable
fun Rosquilla(
    porciones: List<Porcion>,
    centroArriba: String,
    centroAbajo: String,
    tamano: Dp = 180.dp,
) {
    val total = porciones.sumOf { it.valor }.coerceAtLeast(1L)
    val fondo = MaterialTheme.colorScheme.surfaceVariant
    Box(Modifier.size(tamano), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(tamano)) {
            val grosor = size.minDimension * 0.16f
            val arco = Size(size.width - grosor, size.height - grosor)
            val origen = Offset(grosor / 2, grosor / 2)
            drawArc(fondo, 0f, 360f, false, origen, arco, style = Stroke(grosor))
            var inicio = -90f
            val hueco = if (porciones.count { it.valor > 0 } > 1) 1.5f else 0f
            porciones.filter { it.valor > 0 }.forEach { p ->
                val barrido = 360f * p.valor / total
                drawArc(
                    p.color, inicio + hueco / 2, (barrido - hueco).coerceAtLeast(0.5f), false,
                    origen, arco, style = Stroke(grosor, cap = StrokeCap.Butt)
                )
                inicio += barrido
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centroArriba, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(centroAbajo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

data class Serie(val valores: List<Long?>, val color: Color, val discontinua: Boolean = false)

/**
 * Gráfico de líneas sencillo. Todas las series comparten eje X (índice) y escala.
 * Un valor null corta la línea (p. ej. días futuros).
 */
@Composable
fun Lineas(series: List<Serie>, modifier: Modifier = Modifier, alto: Dp = 160.dp) {
    val maximo = series.flatMap { it.valores }.filterNotNull().maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val n = series.maxOf { it.valores.size }.coerceAtLeast(2)
    val rejilla = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier.fillMaxWidth().height(alto)) {
        val pasoX = size.width / (n - 1)
        for (i in 0..3) {
            val y = size.height * i / 3f
            drawLine(rejilla, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        series.forEach { s ->
            val camino = Path()
            var empezado = false
            s.valores.forEachIndexed { i, v ->
                if (v == null) { empezado = false; return@forEachIndexed }
                val x = i * pasoX
                val y = size.height - size.height * (v.toFloat() / maximo) * 0.95f
                if (!empezado) { camino.moveTo(x, y); empezado = true } else camino.lineTo(x, y)
            }
            drawPath(
                camino, s.color,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = if (s.discontinua) PathEffect.dashPathEffect(floatArrayOf(12f, 10f)) else null,
                )
            )
            s.valores.withIndex().lastOrNull { it.value != null }?.let { (i, v) ->
                drawCircle(s.color, 4.dp.toPx(), Offset(i * pasoX, size.height - size.height * (v!!.toFloat() / maximo) * 0.95f))
            }
        }
    }
}
