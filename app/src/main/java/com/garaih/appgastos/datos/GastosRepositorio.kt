package com.garaih.appgastos.datos

import android.content.Context
import com.garaih.appgastos.captura.NotificacionCruda
import kotlinx.coroutines.flow.Flow

class GastosRepositorio(private val dao: GastoDao) {

    val gastos: Flow<List<Gasto>> = dao.observarTodos()

    /** Convierte una notificación de la cartera en Gasto y lo guarda. Devuelve false si era duplicado. */
    suspend fun guardarDesdeNotificacion(n: NotificacionCruda, claveOrigen: String): Boolean {
        val r = ParserCartera.interpretar(n.titulo, n.texto, n.textoGrande, n.subTexto)
        val gasto = Gasto(
            fecha = n.momento,
            importeCentimos = r.importeCentimos,
            moneda = r.moneda,
            comercio = r.comercio ?: "Sin interpretar",
            tarjeta = r.tarjeta,
            origen = Gasto.ORIGEN_CARTERA,
            claveOrigen = claveOrigen,
            textoCrudo = if (r.interpretado) null else
                listOfNotNull(n.titulo, n.texto, n.textoGrande, n.subTexto).joinToString(" | "),
            interpretado = r.interpretado,
        )
        return dao.insertar(gasto) != -1L
    }

    suspend fun insertarEjemplo() {
        dao.insertar(
            Gasto(
                fecha = System.currentTimeMillis(),
                importeCentimos = 1234,
                comercio = "Comercio de prueba",
                tarjeta = "0000",
                origen = Gasto.ORIGEN_PRUEBA,
            )
        )
    }

    suspend fun borrarPruebas() = dao.borrarPorOrigen(Gasto.ORIGEN_PRUEBA)

    companion object {
        fun de(context: Context) = GastosRepositorio(BaseDatos.obtener(context).gastoDao())
    }
}
