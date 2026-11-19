package com.garaih.appgastos.datos

import java.text.Normalizer

/**
 * Categorización sencilla por palabras clave del comercio.
 * Las reglas del usuario (ReglaCategoria) tienen prioridad y se consultan aparte.
 */
object Clasificador {

    private val palabras: List<Pair<Categoria, List<String>>> = listOf(
        Categoria.COCHE to listOf(
            "gasolinera", "repsol", "cepsa", "moeve", "galp", "shell", "petronor", "ballenoil",
            "plenoil", "petroprix", "bp ", "carburante", "combustible", "estacion de servicio",
            "e.s.", "peaje", "autopista", "audasa", "autoestradas", "parking", "aparcamiento",
            "taller", "neumatico", "itv", "lavado", "midas", "norauto", "feu vert"
        ),
        Categoria.SUPERMERCADO to listOf(
            "mercadona", "gadis", "froiz", "eroski", "carrefour", "lidl", "aldi", "dia ",
            "alcampo", "familia", "hipercor", "supermercado", "super ", "consum", "vegalsa",
            "claudio", "el corte ingles supermercado"
        ),
        Categoria.RESTAURANTES to listOf(
            "restaurante", "bar ", "cafeteria", "cafe", "pizzeria", "burger", "mcdonald",
            "telepizza", "kfc", "taberna", "meson", "pulperia", "tapas", "just eat", "glovo",
            "uber eats", "churreria", "pasteleria", "panaderia"
        ),
        Categoria.ONLINE to listOf(
            "amazon", "amzn", "aliexpress", "shein", "temu", "zalando", "pccomponentes",
            "ebay", "wallapop", "vinted", "paypal"
        ),
        Categoria.OCIO to listOf(
            "cine", "steam", "playstation", "nintendo", "xbox", "netflix", "spotify", "hbo",
            "disney", "prime video", "twitch", "teatro", "concierto", "entradas", "ticketmaster",
            "gimnasio", "museo"
        ),
        Categoria.CASA to listOf(
            "ikea", "leroy merlin", "bricomart", "brico", "ferreteria", "mediamarkt",
            "iberdrola", "endesa", "naturgy", "fenosa", "movistar", "vodafone", "orange",
            "digi", "agua", "butano", "comunidad", "zara home", "jysk", "conforama"
        ),
        Categoria.SALUD to listOf(
            "farmacia", "clinica", "dentista", "optica", "hospital", "fisioterapia", "veterinari"
        ),
    )

    fun normalizar(texto: String): String =
        Normalizer.normalize(texto.lowercase().trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("\\s+"), " ")

    fun sugerir(comercio: String): Categoria? {
        // Espacios alrededor para que "bar " o "dia " no casen dentro de otras palabras.
        val c = " " + normalizar(comercio) + " "
        return palabras.firstOrNull { (_, lista) -> lista.any { c.contains(it) } }?.first
    }
}
