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

    private val PALABRAS_VACIAS = setOf(
        "pago", "tarjeta", "compra", "recibo", "cargo", "factura", "cuota", "suscripcion", "servicio",
        "debito", "credito", "online", "espana", "spain", "europe", "ireland", "limited", "ltd",
    )

    /** true si los dos nombres de comercio comparten alguna palabra significativa (≥ 4 letras). */
    fun mismoComercio(a: String, b: String): Boolean {
        fun palabras(x: String) = Clasificador.normalizar(x).split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 4 && it !in PALABRAS_VACIAS }.toSet()
        val pa = palabras(a)
        val pb = palabras(b)
        return pa.isNotEmpty() && pb.isNotEmpty() && pa.intersect(pb).isNotEmpty()
    }

    /** Un cargo recurrente y un aviso son el mismo pago: mismo importe, o mismo comercio con importe parecido (±20 %). */
    fun encajaConRecurrente(recurrente: Gasto, aviso: Gasto): Boolean {
        if (abs(recurrente.fecha - aviso.fecha) > MARGEN_RECURRENTE_MS) return false
        val a = recurrente.importeCentimos
        val b = aviso.importeCentimos
        if (a != null && a == b) return true
        if (!mismoComercio(recurrente.comercio, aviso.comercio)) return false
        return a == null || b == null || abs(a - b) <= maxOf(a, b) / 5
    }

    /** Gasto ya guardado que corresponde al mismo pago que [nuevo] (llegado por notificación), o null. */
    fun buscarDeNotificacion(nuevo: Gasto, existentes: List<Gasto>): Gasto? {
        val candidatos = existentes.filter { g -> g.id != nuevo.id }
        // 1) Un cargo recurrente ya apuntado (recibo, suscripción, plazo).
        candidatos.filter { it.origen == Gasto.ORIGEN_RECURRENTE && encajaConRecurrente(it, nuevo) }
            .minByOrNull { abs(it.fecha - nuevo.fecha) }?.let { return it }
        // 2) El mismo pago avisado por otra fuente.
        val importe = nuevo.importeCentimos ?: return null
        return candidatos
            .filter { g ->
                g.importeCentimos == importe && g.origen in AUTOMATICAS &&
                    abs(g.fecha - nuevo.fecha) <= margen(g, nuevo)
            }
            .minByOrNull { abs(it.fecha - nuevo.fecha) }
    }

    /**
     * Para un cargo recurrente que se va a apuntar: aviso ya guardado (Wallet, banco o correo)
     * que es ese mismo cargo, aún sin vincular a ninguna recurrencia.
     */
    fun buscarAvisoParaRecurrente(cargo: Gasto, existentes: List<Gasto>): Gasto? =
        existentes.filter { it.origen in AUTOMATICAS && it.recurrenteId == null && encajaConRecurrente(cargo, it) }
            .minByOrNull { abs(it.fecha - cargo.fecha) }

    /**
     * Mismo pago avisado por dos fuentes cuando el importe no se puede comparar (pago en otra
     * moneda: Wallet da "12,10 US$" y el banco la retención en euros). Se casan por tarjeta y hora.
     */
    fun buscarPorTarjeta(nuevo: Gasto, existentes: List<Gasto>): Gasto? {
        val tarjeta = nuevo.tarjeta ?: return null
        return existentes
            .filter { g ->
                g.id != nuevo.id && g.tarjeta == tarjeta && g.origen in AUTOMATICAS && g.origen != nuevo.origen &&
                    ((g.importeCentimos == null) != (nuevo.importeCentimos == null)) &&
                    abs(g.fecha - nuevo.fecha) <= MARGEN_CORREO_MS
            }
            .minByOrNull { abs(it.fecha - nuevo.fecha) }
    }

    /** Une dos avisos del mismo pago: importe en euros de quien lo tenga y el mejor nombre de comercio. */
    fun unir(existente: Gasto, nuevo: Gasto): Gasto {
        // Wallet y el correo traen el nombre del comercio; el aviso del banco a veces no.
        val nombreNuevoMejor = nuevo.origen != Gasto.ORIGEN_SANTANDER &&
            (existente.origen == Gasto.ORIGEN_SANTANDER || existente.comercio == SIN_COMERCIO)
        if (existente.origen == Gasto.ORIGEN_RECURRENTE) {
            // El aviso confirma el cargo real: manda su importe y su fecha.
            val real = nuevo.importeCentimos ?: existente.importeCentimos
            return existente.copy(
                importeCentimos = real,
                fecha = nuevo.fecha,
                tarjeta = existente.tarjeta ?: nuevo.tarjeta,
                interpretado = real != null,
                textoCrudo = null,
            )
        }
        val importe = existente.importeCentimos ?: nuevo.importeCentimos
        return existente.copy(
            importeCentimos = importe,
            moneda = "EUR",
            comercio = if (nombreNuevoMejor && nuevo.comercio != SIN_COMERCIO) nuevo.comercio else existente.comercio,
            tarjeta = existente.tarjeta ?: nuevo.tarjeta,
            interpretado = importe != null,
            textoCrudo = if (importe != null) null else existente.textoCrudo ?: nuevo.textoCrudo,
        )
    }

    /** Nombre provisional cuando el aviso no dice el comercio. */
    const val SIN_COMERCIO = "Pago con tarjeta"

    /** Para altas manuales: mismo importe el mismo día (o con texto de comercio parecido). */
    fun buscarParaManual(nuevo: Gasto, existentes: List<Gasto>, mismoDia: (Long, Long) -> Boolean): Gasto? {
        val importe = nuevo.importeCentimos ?: return null
        return existentes.firstOrNull { g ->
            g.id != nuevo.id && g.importeCentimos == importe && mismoDia(g.fecha, nuevo.fecha)
        }
    }
}
