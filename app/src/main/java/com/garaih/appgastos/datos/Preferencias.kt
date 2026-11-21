package com.garaih.appgastos.datos

import android.content.Context

/** Ajustes sencillos de la app (SharedPreferences, solo en el móvil). */
class Preferencias(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    var diaInicioPeriodo: Int
        get() = prefs.getInt(CLAVE_DIA, 25)
        set(valor) = prefs.edit().putInt(CLAVE_DIA, valor.coerceIn(1, 28)).apply()

    /** Leer también las notificaciones de compra de la app de Santander. */
    var leerSantander: Boolean
        get() = prefs.getBoolean(CLAVE_SANTANDER, true)
        set(valor) = prefs.edit().putBoolean(CLAVE_SANTANDER, valor).apply()

    /** Leer avisos de Gmail de los remitentes elegidos (recibos por correo). */
    var leerCorreo: Boolean
        get() = prefs.getBoolean(CLAVE_CORREO, true)
        set(valor) = prefs.edit().putBoolean(CLAVE_CORREO, valor).apply()

    /** Remitentes de correo aceptados, separados por comas. */
    var remitentesCorreo: String
        get() = prefs.getString(CLAVE_REMITENTES, null) ?: com.garaih.appgastos.captura.Correo.REMITENTES_POR_DEFECTO
        set(valor) = prefs.edit().putString(CLAVE_REMITENTES, valor).apply()

    private companion object {
        const val CLAVE_CORREO = "leer_correo"
        const val CLAVE_REMITENTES = "remitentes_correo"
        const val CLAVE_DIA = "dia_inicio_periodo"
        const val CLAVE_SANTANDER = "leer_santander"
    }
}
