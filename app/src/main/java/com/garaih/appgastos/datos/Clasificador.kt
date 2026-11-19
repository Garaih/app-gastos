package com.garaih.appgastos.datos

import java.text.Normalizer

/**
 * Categorización sencilla por palabras clave del comercio (palabras completas).
 * Las reglas del usuario (ReglaCategoria) tienen prioridad y se consultan aparte.
 */
object Clasificador {

    data class Sugerencia(val categoria: String, val subcategoria: String?)

    fun normalizar(texto: String): String =
        Normalizer.normalize(texto.lowercase().trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("\\s+"), " ")

    /** true si la palabra (o frase) aparece completa dentro del comercio. */
    fun contiene(comercioNormalizado: String, palabra: String): Boolean {
        val p = normalizar(palabra)
        if (p.isEmpty()) return false
        return Regex("(^|[^a-z0-9])" + Regex.escape(p) + "([^a-z0-9]|$)").containsMatchIn(comercioNormalizado)
    }

    fun sugerir(comercio: String, categorias: List<Categoria>): Sugerencia? {
        val c = normalizar(comercio)
        for (cat in categorias.sortedBy { it.orden }) {
            cat.listaSubcategorias().firstOrNull { sub -> sub.palabras.any { contiene(c, it) } }
                ?.let { return Sugerencia(cat.clave, it.nombre) }
            if (cat.listaPalabras().any { contiene(c, it) }) return Sugerencia(cat.clave, null)
        }
        return null
    }
}
