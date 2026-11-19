package com.garaih.appgastos.captura

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Almacén en memoria de las notificaciones capturadas durante el spike.
 * No se guarda nada en disco: al cerrar el proceso se pierde.
 */
object CapturaRepositorio {
    private const val MAXIMO = 50

    private val _notificaciones = MutableStateFlow<List<NotificacionCruda>>(emptyList())
    val notificaciones: StateFlow<List<NotificacionCruda>> = _notificaciones.asStateFlow()

    fun anadir(n: NotificacionCruda) {
        _notificaciones.update { actual ->
            // Una misma notificación puede actualizarse: sustituimos por clave.
            (listOf(n) + actual.filterNot { it.clave == n.clave && it.momento == n.momento })
                .take(MAXIMO)
        }
    }

    fun borrar() {
        _notificaciones.value = emptyList()
    }
}
