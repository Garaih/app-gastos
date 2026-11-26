package com.garaih.appgastos.recordatorios

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.garaih.appgastos.MainActivity
import com.garaih.appgastos.R
import com.garaih.appgastos.datos.GastosRepositorio
import com.garaih.appgastos.datos.Recurrencia
import com.garaih.appgastos.datos.Recurrente
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Avisos antes de los cargos recurrentes (y para acordarse de dar de baja suscripciones). */
object Recordatorios {
    private const val CANAL = "recordatorios"
    private const val TRABAJO = "recordatorios-diarios"

    /** Programa la revisión periódica (unas dos veces al día). Se llama al abrir la app. */
    fun programar(context: Context) {
        crearCanal(context)
        val peticion = PeriodicWorkRequestBuilder<TrabajoRecordatorios>(12, TimeUnit.HOURS).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(TRABAJO, ExistingPeriodicWorkPolicy.KEEP, peticion)
    }

    fun puedeNotificar(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Texto del aviso, o null si para este recurrente no toca avisar hoy. */
    fun aviso(r: Recurrente, hoy: LocalDate): Pair<String, String>? {
        val dias = r.recordarDias ?: return null
        val sig = Recurrencia.siguiente(r) ?: return null
        val faltan = ChronoUnit.DAYS.between(hoy, sig.fecha)
        if (faltan < 0 || faltan > dias) return null
        if (r.avisadoMes == Recurrencia.aClave(sig.mes)) return null
        val importe = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES")).format(r.importeCentimos / 100.0)
        val cuando = when (faltan) {
            0L -> "hoy"
            1L -> "mañana"
            else -> "el día ${sig.fecha.dayOfMonth} (en $faltan días)"
        }
        return if (r.quiereCancelar) {
            "Da de baja ${r.comercio}" to "Si no la cancelas, se cobrarán $importe $cuando."
        } else {
            "Cargo próximo: ${r.comercio}" to "Se cobrarán $importe $cuando."
        }
    }

    suspend fun revisar(context: Context) {
        val repo = GastosRepositorio.de(context)
        val hoy = LocalDate.now()
        repo.recurrentesActivos().forEach { r ->
            val (titulo, texto) = aviso(r, hoy) ?: return@forEach
            if (mostrar(context, r.id.toInt(), titulo, texto)) {
                Recurrencia.siguiente(r)?.let { repo.marcarAvisado(r, Recurrencia.aClave(it.mes)) }
            }
        }
    }

    private fun crearCanal(context: Context) {
        val canal = NotificationChannel(CANAL, "Recordatorios de cargos", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Avisos antes de recibos, suscripciones y plazos"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    private fun mostrar(context: Context, id: Int, titulo: String, texto: String): Boolean {
        if (!puedeNotificar(context)) return false
        crearCanal(context)
        val abrir = PendingIntent.getActivity(
            context, id, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(10_000 + id, n)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}

class TrabajoRecordatorios(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Recordatorios.revisar(applicationContext)
        return Result.success()
    }
}
