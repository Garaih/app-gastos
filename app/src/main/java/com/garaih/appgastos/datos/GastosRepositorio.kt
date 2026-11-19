package com.garaih.appgastos.datos

import android.content.Context
import com.garaih.appgastos.captura.NotificacionCruda
import kotlinx.coroutines.flow.Flow

class GastosRepositorio(
    private val dao: GastoDao,
    private val reglas: ReglaCategoriaDao,
) {

    val gastos: Flow<List<Gasto>> = dao.observarTodos()

    /** Regla del usuario para ese comercio; si no hay, palabras clave. */
    suspend fun categoriaPara(comercio: String): String? =
        reglas.buscar(Clasificador.normalizar(comercio))?.categoria
            ?: Clasificador.sugerir(comercio)?.clave

    /** Convierte una notificación de la cartera en Gasto y lo guarda. Devuelve false si era duplicado. */
    suspend fun guardarDesdeNotificacion(n: NotificacionCruda, claveOrigen: String): Boolean {
        val r = ParserCartera.interpretar(n.titulo, n.texto, n.textoGrande, n.subTexto)
        val comercio = r.comercio ?: "Sin interpretar"
        val gasto = Gasto(
            fecha = n.momento,
            importeCentimos = r.importeCentimos,
            moneda = r.moneda,
            comercio = comercio,
            tarjeta = r.tarjeta,
            origen = Gasto.ORIGEN_CARTERA,
            claveOrigen = claveOrigen,
            textoCrudo = if (r.interpretado) null else
                listOfNotNull(n.titulo, n.texto, n.textoGrande, n.subTexto).joinToString(" | "),
            interpretado = r.interpretado,
            categoria = if (r.comercio != null) categoriaPara(comercio) else null,
        )
        return dao.insertar(gasto) != -1L
    }

    /** Alta o edición manual. Si recordar, la categoría se aplica a ese comercio en adelante. */
    suspend fun guardar(gasto: Gasto, recordarCategoria: Boolean) {
        val corregido = if (gasto.importeCentimos != null) gasto.copy(interpretado = true, textoCrudo = null) else gasto
        if (corregido.id == 0L) dao.insertar(corregido) else dao.actualizar(corregido)

        val cat = corregido.categoria
        if (recordarCategoria && cat != null) {
            val clave = Clasificador.normalizar(corregido.comercio)
            reglas.guardar(ReglaCategoria(clave, cat))
            // También a los gastos de ese comercio que aún no tienen categoría.
            dao.sinCategoria()
                .filter { Clasificador.normalizar(it.comercio) == clave }
                .forEach { dao.actualizar(it.copy(categoria = cat)) }
        }
    }

    suspend fun borrar(gasto: Gasto) = dao.borrar(gasto)

    suspend fun insertarEjemplo() {
        dao.insertar(
            Gasto(
                fecha = System.currentTimeMillis(),
                importeCentimos = 1234,
                comercio = "Comercio de prueba",
                tarjeta = "0000",
                origen = Gasto.ORIGEN_PRUEBA,
                categoria = Categoria.OTROS.clave,
            )
        )
    }

    suspend fun borrarPruebas() = dao.borrarPorOrigen(Gasto.ORIGEN_PRUEBA)

    companion object {
        fun de(context: Context): GastosRepositorio {
            val db = BaseDatos.obtener(context)
            return GastosRepositorio(db.gastoDao(), db.reglaDao())
        }
    }
}
