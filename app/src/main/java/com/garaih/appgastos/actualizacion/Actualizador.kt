package com.garaih.appgastos.actualizacion

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Busca la última versión publicada en GitHub Releases (repositorio público) y la instala.
 * Solo se conecta a GitHub; no envía ningún dato de gastos.
 */
object Actualizador {
    private const val API = "https://api.github.com/repos/Garaih/app-gastos/releases/latest"

    data class Version(
        val codigo: Long,
        val nombre: String,
        val notas: String,
        val urlApk: String,
    )

    sealed interface Resultado {
        data class Disponible(val version: Version) : Resultado
        data class AlDia(val codigoActual: Long) : Resultado
        data class Error(val mensaje: String) : Resultado
    }

    fun codigoInstalado(context: Context): Long =
        PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0))

    suspend fun buscar(context: Context): Resultado = withContext(Dispatchers.IO) {
        try {
            val conexion = (URL(API).openConnection() as HttpURLConnection).apply {
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            val codigoHttp = conexion.responseCode
            if (codigoHttp == 404) {
                return@withContext Resultado.Error("No hay versiones publicadas o el repositorio no es público.")
            }
            if (codigoHttp != 200) return@withContext Resultado.Error("GitHub respondió $codigoHttp.")
            val json = JSONObject(conexion.inputStream.bufferedReader().use { it.readText() })
            val etiqueta = json.optString("tag_name")
            val codigo = etiqueta.removePrefix("v").toLongOrNull()
                ?: return@withContext Resultado.Error("Versión publicada sin número ($etiqueta).")
            val assets = json.optJSONArray("assets")
            var url: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name").endsWith(".apk")) { url = a.optString("browser_download_url"); break }
                }
            }
            if (url.isNullOrEmpty()) return@withContext Resultado.Error("La versión publicada no tiene APK.")
            val actual = codigoInstalado(context)
            if (codigo > actual) {
                Resultado.Disponible(
                    Version(codigo, json.optString("name", etiqueta), json.optString("body", ""), url)
                )
            } else {
                Resultado.AlDia(actual)
            }
        } catch (e: Exception) {
            Resultado.Error("Sin conexión o error al consultar GitHub.")
        }
    }

    /** Descarga el APK a la caché. onProgreso recibe 0..1 (o -1 si no se conoce el tamaño). */
    suspend fun descargar(context: Context, version: Version, onProgreso: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "actualizaciones").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val destino = File(dir, "app-gastos-${version.codigo}.apk")
            var url = URL(version.urlApk)
            var conexion: HttpURLConnection
            // GitHub redirige a otro dominio; seguimos las redirecciones a mano.
            var saltos = 0
            while (true) {
                conexion = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 15_000
                    readTimeout = 30_000
                }
                val c = conexion.responseCode
                if (c in 300..399 && saltos < 5) {
                    url = URL(conexion.getHeaderField("Location")); saltos++; conexion.disconnect()
                } else break
            }
            if (conexion.responseCode != 200) error("Descarga fallida (${conexion.responseCode})")
            val total = conexion.contentLengthLong
            conexion.inputStream.use { entrada ->
                destino.outputStream().use { salida ->
                    val buffer = ByteArray(64 * 1024)
                    var leido = 0L
                    while (true) {
                        val n = entrada.read(buffer)
                        if (n < 0) break
                        salida.write(buffer, 0, n)
                        leido += n
                        onProgreso(if (total > 0) leido.toFloat() / total else -1f)
                    }
                }
            }
            destino
        }

    /** true si el usuario ya permitió a esta app instalar aplicaciones. */
    fun puedeInstalar(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    fun abrirPermisoInstalar(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun instalar(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
