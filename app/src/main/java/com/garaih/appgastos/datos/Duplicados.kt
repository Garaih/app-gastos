package com.garaih.appgastos.datos

import kotlin.math.abs

/** Detección de gastos repetidos (mismo pago avisado por dos fuentes o apuntado dos veces). */
object Duplicados {
    /** Margen para considerar que dos avisos de fuentes distintas son el mismo pago. */
    const val MARGEN_FUENTES_MS = 15 * 60 * 1000L
    /** Margen dentro de la misma fuente (avisos repetidos de una misma app). */
    const val MARGEN_MISMA_FUENTE_MS = 2 * 60 * 1000L

    private val AUTOMATICAS = setOf(Gasto.ORIGEN_CARTERA, Gasto.ORIGEN_SANTANDER)

    /** Gasto ya guardado que corresponde al mismo pago que [nuevo] (llegado por notificación), o null. */
    fun buscarDeNotificacion(nuevo: Gasto, existentes: List<Gasto>): Gasto? {
        val importe = nuevo.importeCentimos ?: return null
        return existentes.firstOrNull { g ->
            g.id != nuevo.id && g.importeCentimos == importe && g.origen in AUTOMATICAS &&
                abs(g.fecha - nuevo.fecha) <= (if (g.origen == nuevo.origen) MARGEN_MISMA_FUENTE_MS else MARGEN_FUENTES_MS)
        }
    }

    /** Para altas manuales: mismo importe el mismo día (o con texto de comercio parecido). */
    fun buscarParaManual(nuevo: Gasto, existentes: List<Gasto>, mismoDia: (Long, Long) -> Boolean): Gasto? {
        val importe = nuevo.importeCentimos ?: return null
        return existentes.firstOrNull { g ->
            g.id != nuevo.id && g.importeCentimos == importe && mismoDia(g.fecha, nuevo.fecha)
        }
    }
}
