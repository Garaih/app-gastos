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
        instancia = this
        preferencias.marcarLectorConectado()
        // Recoge las que ya estuvieran en la bandeja (los duplicados se ignoran).
        scope.launch { revisarBandeja() }
    }

    override fun onListenerDisconnected() {
        if (instancia === this) instancia = null
        super.onListenerDisconnected()
    }

    /** Procesa los avisos que hay ahora en la bandeja. Devuelve (avisos revisados, gastos nuevos). */
    suspend fun revisarBandeja(): Pair<Int, Int> {
        val lista = runCatching { activeNotifications }.getOrNull().orEmpty().filter { it.packageName in paquetes() }
        var nuevos = 0
        lista.forEach { if (procesarYGuardar(it)) nuevos++ }
        return lista.size to nuevos
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in paquetes()) return
        procesar(sbn)
    }

    override fun onDestroy() {
        if (instancia === this) instancia = null
        scope.cancel()
        super.onDestroy()
    }

    private fun procesar(sbn: StatusBarNotification) {
        scope.launch { procesarYGuardar(sbn) }
    }

    /** true si se ha guardado un gasto nuevo. */
    private suspend fun procesarYGuardar(sbn: StatusBarNotification): Boolean {
        val notificacion = sbn.notification
        // Las notificaciones de resumen de grupo no son pagos.
        if (notificacion.flags and Notification.FLAG_GROUP_SUMMARY != 0) return false

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
            Correo.remitenteDe(cruda.titulo, Correo.remitentes(preferencias.remitentesCorreo)) ?: return false
            val contenido = listOfNotNull(cruda.texto, cruda.textoGrande, cruda.subTexto).joinToString("\n")
            if (!Correo.pareceRecibo(contenido)) return false
            if (com.garaih.appgastos.datos.ParserCartera.buscarImporte(contenido) == null) return false
        }
        CapturaRepositorio.anadir(cruda)
        preferencias.marcarAvisoRecibido()
        // Misma notificación y mismo instante = mismo pago, aunque se reenvíe o actualice.
        return repositorio.guardarDesdeNotificacion(
            cruda,
            claveOrigen = "${sbn.key}|$momento",
            origen = Cartera.origenDe(sbn.packageName),
        )
    }

    companion object {
        /** Servicio en marcha, o null si el sistema lo ha parado (p. ej. ahorro de batería de MIUI). */
        @Volatile var instancia: ServicioNotificaciones? = null
            private set
    }
}
