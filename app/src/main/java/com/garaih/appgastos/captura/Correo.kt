package com.garaih.appgastos.captura

import com.garaih.appgastos.datos.Clasificador

/**
 * Filtro para los avisos de Gmail: solo remitentes elegidos y solo correos que parezcan
 * un recibo (con importe y sin pinta de promoción). Todo lo demás se ignora y no se guarda.
 */
object Correo {
    const val PAQUETE_GMAIL = "com.google.android.gm"

    const val REMITENTES_POR_DEFECTO =
        "PayPal, Google Play, Google Payments, Amazon, Orange, Stripe, AliExpress, Just Eat, Glovo, " +
            "Uber, Renfe, Iberdrola, Endesa, Naturgy, Movistar, Vodafone, Digi, Steam, Apple"

    private val PARECE_RECIBO = Regex(
        """(?i)\b(recibo|factura|pago|pagado|has pagado|compra|pedido|cargo|cobro|importe|total|suscripci[oó]n|receipt|order|payment|invoice)\b"""
    )
    private val PARECE_PROMO = Regex(
        """(?i)(\boferta|\bdescuento|\bahorra|\bpromo|\bgana\b|\bregalo|\bcup[oó]n|\bsorteo|\bblack friday|\bc[oó]digo\b|\d+\s?%)"""
    )

    fun remitentes(texto: String): List<String> =
        texto.split(',').map { Clasificador.normalizar(it) }.filter { it.isNotEmpty() }

    /** Remitente que coincide con el título de la notificación de Gmail (nombre de quien envía), o null. */
    fun remitenteDe(titulo: String?, lista: List<String>): String? {
        val t = Clasificador.normalizar(titulo ?: return null)
        return lista.firstOrNull { Clasificador.contiene(t, it) }
    }

    /** true si el texto parece un recibo y no una promoción. */
    fun pareceRecibo(texto: String): Boolean = PARECE_RECIBO.containsMatchIn(texto) && !PARECE_PROMO.containsMatchIn(texto)
}
