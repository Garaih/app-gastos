package com.garaih.appgastos.datos

import android.content.Context

/** Ajustes sencillos de la app (SharedPreferences, solo en el móvil). */
class Preferencias(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    var diaInicioPeriodo: Int
        get() = prefs.getInt(CLAVE_DIA, 25)
        set(valor) = prefs.edit().putInt(CLAVE_DIA, valor.coerceIn(1, 28)).apply()

    private companion object {
        const val CLAVE_DIA = "dia_inicio_periodo"
    }
}
