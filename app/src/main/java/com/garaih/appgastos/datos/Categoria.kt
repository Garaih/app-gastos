package com.garaih.appgastos.datos

/** Categorías de gasto. Se guarda la clave en Gasto.categoria. */
enum class Categoria(val clave: String, val nombre: String, val emoji: String, val color: Long) {
    COCHE("coche", "Coche", "🚗", 0xFF3B82F6),
    SUPERMERCADO("super", "Supermercados", "🛒", 0xFF22A55E),
    CASA("casa", "Casa", "🏠", 0xFFF59E0B),
    RESTAURANTES("restaurantes", "Restaurantes y bares", "🍽️", 0xFFEF4444),
    OCIO("ocio", "Ocio", "🎮", 0xFFA855F7),
    ONLINE("online", "Compras online", "📦", 0xFF06B6D4),
    SALUD("salud", "Salud", "💊", 0xFFEC4899),
    OTROS("otros", "Otros", "🧾", 0xFF64748B);

    companion object {
        fun desdeClave(clave: String?): Categoria? = entries.firstOrNull { it.clave == clave }
    }
}
