package com.garaih.appgastos.datos

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs

/** Detecta pagos que se repiten cada mes y aún no están marcados como recurrentes. */
object Suscripciones {

    data class Sugerencia(
        val comercio: String,
        val importeCentimos: Long,
        val dia: Int,
        val meses: Int,
        val ultimo: Gasto,
        val gastos: List<Gasto>,
    )

    private val ORIGENES = setOf(Gasto.ORIGEN_CARTERA, Gasto.ORIGEN_SANTANDER, Gasto.ORIGEN_CORREO, Gasto.ORIGEN_MANUAL)

    /**
     * Mismo comercio en al menos 2 meses distintos de los últimos 4, con importes parecidos (±15 %),
     * y sin ser ya un gasto recurrente.
     */
    fun detectar(
        gastos: List<Gasto>,
        recurrentes: List<Recurrente>,
        hoy: YearMonth = YearMonth.now(),
        zona: ZoneId = ZoneId.systemDefault(),
    ): List<Sugerencia> {
        val desde = hoy.minusMonths(3)
        val yaRecurrentes = recurrentes.map { Clasificador.normalizar(it.comercio) }.toSet()
        fun mes(g: Gasto) = YearMonth.from(Instant.ofEpochMilli(g.fecha).atZone(zona))
        return gastos
            .asSequence()
            .filter { it.recurrenteId == null && it.importeCentimos != null && it.origen in ORIGENES }
            .filter { it.comercio != Duplicados.SIN_COMERCIO && !mes(it).isBefore(desde) }
            .groupBy { Clasificador.normalizar(it.comercio) }
            .filterKeys { it !in yaRecurrentes && yaRecurrentes.none { r -> Duplicados.mismoComercio(r, it) } }
            .mapNotNull { (_, lista) ->
                val porMes = lista.groupBy { mes(it) }
                if (porMes.size < 2) return@mapNotNull null
                // Un pago por mes (el más reciente de cada mes) e importes parecidos.
                val uno = porMes.values.map { l -> l.maxBy { it.fecha } }.sortedBy { it.fecha }
                val importes = uno.map { it.importeCentimos!! }
                val max = importes.max()
                if (importes.any { abs(it - max) > max * 15 / 100 }) return@mapNotNull null
                val ultimo = uno.last()
                Sugerencia(
                    comercio = ultimo.comercio,
                    importeCentimos = ultimo.importeCentimos!!,
                    dia = Instant.ofEpochMilli(ultimo.fecha).atZone(zona).dayOfMonth.coerceAtMost(28),
                    meses = uno.size,
                    ultimo = ultimo,
                    gastos = lista,
                )
            }
            .sortedByDescending { it.importeCentimos }
            .toList()
    }
}
