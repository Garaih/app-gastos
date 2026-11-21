package com.garaih.appgastos.captura

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.garaih.appgastos.datos.GastosRepositorio
import com.garaih.appgastos.datos.Preferencias
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Escucha notificaciones y descarta todo lo que no venga de Google Wallet o (si está activado)
 * de la app de Santander. Las aceptadas se muestran en el visor (memoria) y se guardan como Gasto (Room).
 * No escribe su contenido en el log.
 */
class ServicioNotificaciones : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repositorio by lazy { GastosRepositorio.de(applicationContext) }
    private val preferencias by lazy { Preferencias(applicationContext) }
    private fun paquetes() = Cartera.paquetes(preferencias.leerSantander, preferencias.leerCorreo)

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Recoge las de la cartera que ya estuvieran en la bandeja (los duplicados se ignoran).
        runCatching { activeNotifications }.getOrNull()
            ?.filter { it.packageName in paquetes() }
            ?.forEach { procesar(it) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in paquetes()) return
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
        if (sbn.packageName == Correo.PAQUETE_GMAIL) {
            // Solo remitentes elegidos y solo si parece un recibo con importe; si no, se ignora del todo.
            Correo.remitenteDe(cruda.titulo, Correo.remitentes(preferencias.remitentesCorreo)) ?: return
            val contenido = listOfNotNull(cruda.texto, cruda.textoGrande, cruda.subTexto).joinToString("\n")
            if (!Correo.pareceRecibo(contenido)) return
            if (com.garaih.appgastos.datos.ParserCartera.buscarImporte(contenido) == null) return
        }
        CapturaRepositorio.anadir(cruda)
        scope.launch {
            // Misma notificación y mismo instante = mismo pago, aunque se reenvíe o actualice.
            repositorio.guardarDesdeNotificacion(
                cruda,
                claveOrigen = "${sbn.key}|$momento",
                origen = Cartera.origenDe(sbn.packageName),
            )
        }
    }
}
