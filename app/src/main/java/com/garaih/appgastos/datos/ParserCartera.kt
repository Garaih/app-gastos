package com.garaih.appgastos.datos

/**
 * Interpreta el texto de una notificación de pago de la cartera.
 * Flexible a propósito: el formato exacto se confirmará con un pago real.
 */
object ParserCartera {

    data class Resultado(
        val importeCentimos: Long?,
        val moneda: String,
        val comercio: String?,
        val tarjeta: String?,
    ) {
        val interpretado: Boolean get() = importeCentimos != null
    }

    // Número con separadores de miles y decimales en formato español o inglés.
    private const val NUMERO = """\d{1,3}(?:[.\s ]\d{3})+(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?"""
    private val IMPORTE_SUFIJO = Regex("""($NUMERO)\s?(€|EUR)""", RegexOption.IGNORE_CASE)
    private val IMPORTE_PREFIJO = Regex("""(€|EUR)\s?($NUMERO)""", RegexOption.IGNORE_CASE)
    private val TARJETA = Regex(
        """(?:[•*·]{2,}\s?|terminada en\s|acabada en\s|ending in\s|final\s)(\d{4})\b""",
        RegexOption.IGNORE_CASE
    )

    fun interpretar(titulo: String?, texto: String?, textoGrande: String?, subTexto: String?): Resultado {
        val cuerpo = listOfNotNull(texto, textoGrande, subTexto).joinToString("\n")
        val todo = listOfNotNull(titulo, cuerpo).joinToString("\n")

        val importe = buscarImporte(cuerpo) ?: buscarImporte(titulo.orEmpty())
        val tarjeta = TARJETA.find(todo)?.groupValues?.get(1)

        // El comercio suele ir en el título; si el título es el importe o un texto genérico del
        // banco ("Compra con tarjeta", "Santander"...), se busca "... en COMERCIO ..." en el texto.
        val tituloLimpio = titulo?.trim()?.takeIf { it.isNotEmpty() }
        val tituloGenerico = tituloLimpio != null && GENERICO.containsMatchIn(tituloLimpio)
        val comercio = when {
            tituloLimpio != null && !tituloGenerico && buscarImporte(tituloLimpio) == null -> tituloLimpio
            else -> comercioEnTexto(cuerpo) ?: texto?.lineSequence()?.map { it.trim() }
                ?.firstOrNull { it.isNotEmpty() && buscarImporte(it) == null && !GENERICO.containsMatchIn(it) }
        }

        return Resultado(
            importeCentimos = importe?.first,
            moneda = importe?.second ?: "EUR",
            comercio = comercio,
            tarjeta = tarjeta,
        )
    }

    private val GENERICO = Regex(
        """(?i)\b(santander|compra|pago|tarjeta|cargo|operaci[oó]n|movimiento|wallet|aviso|alerta)\b"""
    )
    private val EN_COMERCIO = Regex("""(?i)\ben\s+(.+?)(?=\s+(?:con|el|por|desde|a las|mediante)\b|[.,;(]|$)""")

    /** "Compra de 12,34 € en MERCADONA con tu tarjeta" -> "MERCADONA". */
    internal fun comercioEnTexto(texto: String): String? =
        EN_COMERCIO.findAll(texto).map { it.groupValues[1].trim() }
            .firstOrNull { c -> c.isNotEmpty() && !c.first().isDigit() && !c.startsWith("tu ", true) && !c.startsWith("tarjeta", true) }

    /** Devuelve (céntimos, moneda) o null. */
    internal fun buscarImporte(texto: String): Pair<Long, String>? {
        val sufijo = IMPORTE_SUFIJO.find(texto)
        val prefijo = IMPORTE_PREFIJO.find(texto)
        val numero = when {
            sufijo != null && (prefijo == null || sufijo.range.first <= prefijo.range.first) ->
                sufijo.groupValues[1]
            prefijo != null -> prefijo.groupValues[2]
            else -> return null
        }
        return aCentimos(numero)?.let { it to "EUR" }
    }

    /** "1.234,56" -> 123456, "1,234.56" -> 123456, "12,3" -> 1230, "1.234" -> 123400. */
    internal fun aCentimos(bruto: String): Long? {
        val s = bruto.replace(" ", "").replace(" ", "")
        val ultimoSep = maxOf(s.lastIndexOf(','), s.lastIndexOf('.'))
        val (entero, decimales) = if (ultimoSep >= 0 && s.length - ultimoSep - 1 in 1..2) {
            s.substring(0, ultimoSep) to s.substring(ultimoSep + 1)
        } else {
            s to ""
        }
        val enteroLimpio = entero.replace(".", "").replace(",", "")
        if (enteroLimpio.isEmpty() || !enteroLimpio.all { it.isDigit() }) return null
        val dec = decimales.padEnd(2, '0')
        return enteroLimpio.toLong() * 100 + dec.toLong()
    }
}
