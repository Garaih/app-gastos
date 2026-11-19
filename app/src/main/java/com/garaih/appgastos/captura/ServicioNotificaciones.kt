package com.garaih.appgastos.captura

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Escucha notificaciones y descarta todo lo que no venga de la cartera de Google.
 * No registra en el log ni guarda en disco el contenido.
 */
class ServicioNotificaciones : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Recoge las de la cartera que ya estuvieran en la bandeja.
        runCatching { activeNotifications }.getOrNull()
            ?.filter { it.packageName in Cartera.PAQUETES }
            ?.forEach { procesar(it) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in Cartera.PAQUETES) return
        procesar(sbn)
    }

    private fun procesar(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        fun texto(clave: String): String? = extras.getCharSequence(clave)?.toString()
        CapturaRepositorio.anadir(
            NotificacionCruda(
                clave = sbn.key,
                paquete = sbn.packageName,
                momento = sbn.postTime,
                titulo = texto(Notification.EXTRA_TITLE),
                texto = texto(Notification.EXTRA_TEXT),
                textoGrande = texto(Notification.EXTRA_BIG_TEXT),
                subTexto = texto(Notification.EXTRA_SUB_TEXT),
                textoInfo = texto(Notification.EXTRA_INFO_TEXT),
            )
        )
    }
}
