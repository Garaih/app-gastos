package com.garaih.appgastos.captura

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.garaih.appgastos.datos.GastosRepositorio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Escucha notificaciones y descarta todo lo que no venga de la cartera de Google.
 * Las de la cartera se muestran en el visor (memoria) y se guardan como Gasto (Room).
 * No escribe su contenido en el log.
 */
class ServicioNotificaciones : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repositorio by lazy { GastosRepositorio.de(applicationContext) }

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Recoge las de la cartera que ya estuvieran en la bandeja (los duplicados se ignoran).
        runCatching { activeNotifications }.getOrNull()
            ?.filter { it.packageName in Cartera.PAQUETES }
            ?.forEach { procesar(it) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in Cartera.PAQUETES) return
        procesar(sbn)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun procesar(sbn: StatusBarNotification) {
        val notificacion = sbn.notification
        // Las notificaciones de resumen de grupo no son pagos.
        if (notificacion.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = notificacion.extras
        fun texto(clave: String): String? = extras.getCharSequence(clave)?.toString()
        val momento = notificacion.`when`.takeIf { it > 0 } ?: sbn.postTime
        val cruda = NotificacionCruda(
            clave = sbn.key,
            paquete = sbn.packageName,
            momento = momento,
            titulo = texto(Notification.EXTRA_TITLE),
            texto = texto(Notification.EXTRA_TEXT),
            textoGrande = texto(Notification.EXTRA_BIG_TEXT),
            subTexto = texto(Notification.EXTRA_SUB_TEXT),
            textoInfo = texto(Notification.EXTRA_INFO_TEXT),
        )
        CapturaRepositorio.anadir(cruda)
        scope.launch {
            // Misma notificación y mismo instante = mismo pago, aunque se reenvíe o actualice.
            repositorio.guardarDesdeNotificacion(cruda, claveOrigen = "${sbn.key}|$momento")
        }
    }
}
