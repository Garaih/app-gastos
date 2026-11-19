package com.garaih.appgastos.datos

import android.content.Context
import com.garaih.appgastos.captura.NotificacionCruda
import kotlinx.coroutines.flow.Flow

class GastosRepositorio(
    private val dao: GastoDao,
    private val reglas: ReglaCategoriaDao,
    private val categoriasDao: CategoriaDao,
    private val finanzas: FinanzasDao,
) {
    val presupuestos: Flow<List<Presupuesto>> = finanzas.presupuestos()
    val objetivos: Flow<List<Objetivo>> = finanzas.objetivos()
    val inversiones: Flow<List<Inversion>> = finanzas.inversiones()
    val ingresos: Flow<List<Ingreso>> = finanzas.ingresos()

    suspend fun presupuestoDe(categoria: String): Long? = finanzas.presupuesto(categoria)?.limiteCentimos

    suspend fun guardarPresupuesto(categoria: String, limiteCentimos: Long?) {
        if (limiteCentimos == null || limiteCentimos <= 0) finanzas.borrarPresupuesto(categoria)
        else finanzas.guardarPresupuesto(Presupuesto(categoria, limiteCentimos))
    }

    suspend fun guardarObjetivo(o: Objetivo) { if (o.id == 0L) finanzas.insertarObjetivo(o) else finanzas.actualizarObjetivo(o) }
    suspend fun borrarObjetivo(o: Objetivo) = finanzas.borrarObjetivo(o)

    suspend fun guardarInversion(i: Inversion) { if (i.id == 0L) finanzas.insertarInversion(i) else finanzas.actualizarInversion(i) }
    suspend fun borrarInversion(i: Inversion) = finanzas.borrarInversion(i)

    suspend fun guardarIngreso(i: Ingreso) { if (i.id == 0L) finanzas.insertarIngreso(i) else finanzas.actualizarIngreso(i) }
    suspend fun borrarIngreso(i: Ingreso) = finanzas.borrarIngreso(i)


    val gastos: Flow<List<Gasto>> = dao.observarTodos()
    val categorias: Flow<List<Categoria>> = categoriasDao.observarTodas()

    /** Regla del usuario para ese comercio; si no hay, palabras clave de las categorías. */
    suspend fun sugerencia(comercio: String): Clasificador.Sugerencia? {
        reglas.buscar(Clasificador.normalizar(comercio))?.let {
            return Clasificador.Sugerencia(it.categoria, it.subcategoria)
        }
        return Clasificador.sugerir(comercio, categoriasDao.todas())
    }

    /** Convierte una notificación de la cartera en Gasto y lo guarda. Devuelve false si era duplicado. */
    suspend fun guardarDesdeNotificacion(n: NotificacionCruda, claveOrigen: String): Boolean {
        val r = ParserCartera.interpretar(n.titulo, n.texto, n.textoGrande, n.subTexto)
        val comercio = r.comercio ?: "Sin interpretar"
        val sug = if (r.comercio != null) sugerencia(comercio) else null
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
            categoria = sug?.categoria,
            subcategoria = sug?.subcategoria,
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
            reglas.guardar(ReglaCategoria(clave, cat, corregido.subcategoria))
            // También a los gastos de ese comercio que aún no tienen categoría.
            dao.sinCategoria()
                .filter { Clasificador.normalizar(it.comercio) == clave }
                .forEach { dao.actualizar(it.copy(categoria = cat, subcategoria = corregido.subcategoria)) }
        }
    }

    suspend fun borrar(gasto: Gasto) = dao.borrar(gasto)

    suspend fun guardarCategoria(c: Categoria) {
        val conOrden = if (c.orden == 0) c.copy(orden = categoriasDao.ordenMaximo() + 1) else c
        categoriasDao.guardar(conOrden)
        // Aplica las palabras clave nuevas a los gastos que aún no tienen categoría.
        val todas = categoriasDao.todas()
        dao.sinCategoria().filter { it.interpretado }.forEach { g ->
            Clasificador.sugerir(g.comercio, todas)?.let {
                dao.actualizar(g.copy(categoria = it.categoria, subcategoria = it.subcategoria))
            }
        }
    }

    /** Borra la categoría; sus gastos quedan sin categoría. */
    suspend fun borrarCategoria(c: Categoria) {
        dao.quitarCategoria(c.clave)
        reglas.borrarDeCategoria(c.clave)
        categoriasDao.borrar(c)
        finanzas.borrarPresupuesto(c.clave)
    }

    suspend fun insertarEjemplo() {
        dao.insertar(
            Gasto(
                fecha = System.currentTimeMillis(),
                importeCentimos = 1234,
                comercio = "Comercio de prueba",
                tarjeta = "0000",
                origen = Gasto.ORIGEN_PRUEBA,
                categoria = "otros",
            )
        )
    }

    suspend fun borrarPruebas() = dao.borrarPorOrigen(Gasto.ORIGEN_PRUEBA)

    companion object {
        fun de(context: Context): GastosRepositorio {
            val db = BaseDatos.obtener(context)
            return GastosRepositorio(db.gastoDao(), db.reglaDao(), db.categoriaDao(), db.finanzasDao())
        }
    }
}
