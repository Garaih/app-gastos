package com.garaih.appgastos.datos

import kotlin.math.abs

/** Detección de gastos repetidos (mismo pago avisado por dos fuentes o apuntado dos veces). */
object Duplicados {
    /** Margen para considerar que dos avisos de fuentes distintas son el mismo pago. */
    const val MARGEN_FUENTES_MS = 15 * 60 * 1000L
    /** Margen dentro de la misma fuente (avisos repetidos de una misma app). */
    const val MARGEN_MISMA_FUENTE_MS = 2 * 60 * 1000L
    /** El correo puede llegar bastante después del pago con tarjeta. */
    const val MARGEN_CORREO_MS = 60 * 60 * 1000L
    /** Un recibo por correo frente a un cargo recurrente ya apuntado (domiciliaciones). */
    const val MARGEN_RECURRENTE_MS = 3 * 24 * 60 * 60 * 1000L
    /** Ventana máxima que hay que consultar en la base de datos. */
    const val VENTANA_MAXIMA_MS = MARGEN_RECURRENTE_MS

    private val AUTOMATICAS = setOf(Gasto.ORIGEN_CARTERA, Gasto.ORIGEN_SANTANDER, Gasto.ORIGEN_CORREO)

    private fun margen(existente: Gasto, nuevo: Gasto): Long = when {
        existente.origen == Gasto.ORIGEN_RECURRENTE -> MARGEN_RECURRENTE_MS
        existente.origen == nuevo.origen -> MARGEN_MISMA_FUENTE_MS
        existente.origen == Gasto.ORIGEN_CORREO || nuevo.origen == Gasto.ORIGEN_CORREO -> MARGEN_CORREO_MS
        else -> MARGEN_FUENTES_MS
    }

    /** Gasto ya guardado que corresponde al mismo pago que [nuevo] (llegado por notificación), o null. */
    fun buscarDeNotificacion(nuevo: Gasto, existentes: List<Gasto>): Gasto? {
        val importe = nuevo.importeCentimos ?: return null
        return existentes
            .filter { g ->
                g.id != nuevo.id && g.importeCentimos == importe &&
                    (g.origen in AUTOMATICAS || g.origen == Gasto.ORIGEN_RECURRENTE) &&
                    abs(g.fecha - nuevo.fecha) <= margen(g, nuevo)
            }
            .minByOrNull { abs(it.fecha - nuevo.fecha) }
    }

    /** Para altas manuales: mismo importe el mismo día (o con texto de comercio parecido). */
    fun buscarParaManual(nuevo: Gasto, existentes: List<Gasto>, mismoDia: (Long, Long) -> Boolean): Gasto? {
        val importe = nuevo.importeCentimos ?: return null
        return existentes.firstOrNull { g ->
            g.id != nuevo.id && g.importeCentimos == importe && mismoDia(g.fecha, nuevo.fecha)
        }
    }
}
