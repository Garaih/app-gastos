package com.garaih.appgastos.captura

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object Cartera {
    /** Google Wallet. Único paquete del que se leen notificaciones (verificar en el dispositivo). */
    const val PAQUETE_WALLET = "com.google.android.apps.walletnfcrel"
    /** App Santander España (avisos de compras con tarjeta, también tarjeta física). */
    const val PAQUETE_SANTANDER = "es.bancosantander.apps"

    /** Paquetes de los que se leen notificaciones; el resto se ignora. */
    fun paquetes(leerSantander: Boolean, leerCorreo: Boolean = false): Set<String> = buildSet {
        add(PAQUETE_WALLET)
        if (leerSantander) add(PAQUETE_SANTANDER)
        if (leerCorreo) add(Correo.PAQUETE_GMAIL)
    }

    fun origenDe(paquete: String): String = when (paquete) {
        PAQUETE_SANTANDER -> com.garaih.appgastos.datos.Gasto.ORIGEN_SANTANDER
        Correo.PAQUETE_GMAIL -> com.garaih.appgastos.datos.Gasto.ORIGEN_CORREO
        else -> com.garaih.appgastos.datos.Gasto.ORIGEN_CARTERA
    }

    fun gmailInstalado(context: Context): Boolean = instalado(context, Correo.PAQUETE_GMAIL)

    fun estaInstalada(context: Context): Boolean = instalado(context, PAQUETE_WALLET)
    fun santanderInstalada(context: Context): Boolean = instalado(context, PAQUETE_SANTANDER)

    private fun instalado(context: Context, paquete: String): Boolean = try {
        context.packageManager.getPackageInfo(paquete, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    fun permisoConcedido(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun intentAjustesPermiso(context: Context): Intent {
        val componente = ComponentName(context, ServicioNotificaciones::class.java)
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, componente.flattenToString())
    }

    fun intentAjustesPermisoGeneral(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
}
