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
    val PAQUETES = setOf(PAQUETE_WALLET)

    fun estaInstalada(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(PAQUETE_WALLET, 0)
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
