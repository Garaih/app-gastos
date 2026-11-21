package com.garaih.appgastos.datos

import android.content.Context
import com.garaih.appgastos.captura.NotificacionCruda
import kotlinx.coroutines.flow.Flow

class GastosRepositorio(
    private val dao: GastoDao,
    private val reglas: ReglaCategoriaDao,
    private val categoriasDao: CategoriaDao,
    private val finanzas: FinanzasDao,
    private val recurrentesDao: RecurrenteDao,
) {
    val recurrentes: Flow<List<Recurrente>> = recurrentesDao.observarTodos()

    /** Apunta los cargos de gastos recurrentes que tocan hasta hoy. Se llama al abrir la app. */
    suspend fun generarRecurrentes(hoy: java.time.LocalDate = java.time.LocalDate.now()) {
        recurrentesDao.activos().forEach { r -> generar(r, hoy) }
    }

    private suspend fun generar(r: Recurrente, hoy: java.time.LocalDate) {
        var actual = r
        Recurrencia.pendientes(r, hoy).forEach { c ->
            val fecha = c.fecha.atTime(9, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            dao.insertar(
                Gasto(
                    fecha = fecha,
                    importeCentimos = r.importeCentimos,
                    comercio = r.comercio,
                    origen = Gasto.ORIGEN_RECURRENTE,
                    claveOrigen = "rec:${r.id}:${Recurrencia.aClave(c.mes)}",
                    categoria = r.categoria,
                    subcategoria = r.subcategoria,
                    recurrenteId = r.id,
                    plazo = c.plazo,
                )
            )
            actual = actual.copy(ultimoMes = Recurrencia.aClave(c.mes))
        }
        if (Recurrencia.terminado(actual)) actual = actual.copy(activo = false)
        if (actual != r) recurrentesDao.actualizar(actual)
    }

    /**
     * Guarda un gasto y lo convierte en recurrente (plazos = null, sin fin) desde su fecha.
     * El propio gasto es el plazo 1; los meses siguientes se apuntan solos.
     */
    suspend fun guardarComoRecurrente(gasto: Gasto, plazos: Int?, recordarCategoria: Boolean) {
        val fecha = java.time.Instant.ofEpochMilli(gasto.fecha).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        val mes = Recurrencia.aClave(java.time.YearMonth.from(fecha))
        val id = recurrentesDao.insertar(
            Recurrente(
                comercio = gasto.comercio,
                importeCentimos = gasto.importeCentimos ?: 0,
                categoria = gasto.categoria,
                subcategoria = gasto.subcategoria,
                dia = fecha.dayOfMonth.coerceAtMost(28),
                inicioMes = mes,
                plazos = plazos,
                ultimoMes = mes,
            )
        )
        guardar(gasto.copy(recurrenteId = id, plazo = 1), recordarCategoria)
        recurrentesDao.porId(id)?.let { generar(it, java.time.LocalDate.now()) }
    }

    /** Cambios en la recurrencia: afectan a los cargos que aún no se han apuntado. */
    suspend fun guardarRecurrente(r: Recurrente) {
        val ajustado = r.copy(activo = r.activo && !Recurrencia.terminado(r))
        recurrentesDao.actualizar(ajustado)
        if (ajustado.activo) generar(ajustado, java.time.LocalDate.now())
    }

    suspend fun finalizarRecurrente(r: Recurrente) = recurrentesDao.actualizar(r.copy(activo = false))

    /** Borra la recurrencia; los gastos ya apuntados se conservan. */
    suspend fun borrarRecurrente(r: Recurrente) = recurrentesDao.borrar(r)

    val presupuestos: Flow<List<Presupuesto>> = finanzas.presupuestos()
    val objetivos: Flow<List<Objetivo>> = finanzas.objetivos()
    val inversiones: Flow<List<Inversion>> = finanzas.inversiones()
    val ingresos: Flow<List<Ingreso>> = finanzas.ingresos()
    val ingresosFijos: Flow<List<IngresoFijo>> = finanzas.ingresosFijos()

    /**
     * Fija el importe de un ingreso fijo desde ese periodo en adelante (hasta el siguiente cambio).
     * Si se renombra, el nuevo nombre se aplica a todo su historial.
     */
    suspend fun cambiarIngresoFijo(conceptoAnterior: String?, concepto: String, importeCentimos: Long, periodo: Periodo) {
        if (conceptoAnterior != null && conceptoAnterior != concepto) finanzas.renombrarIngresoFijo(conceptoAnterior, concepto)
        val k = IngresosFijos.clave(periodo)
        val existente = finanzas.ingresoFijo(concepto, k)
        if (existente != null) finanzas.actualizarIngresoFijo(existente.copy(importeCentimos = importeCentimos))
        else finanzas.insertarIngresoFijo(IngresoFijo(concepto = concepto, importeCentimos = importeCentimos, desdeMes = k))
    }

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
    suspend fun guardarDesdeNotificacion(
        n: NotificacionCruda,
        claveOrigen: String,
        origen: String = Gasto.ORIGEN_CARTERA,
    ): Boolean {
        val r = ParserCartera.interpretar(n.titulo, n.texto, n.textoGrande, n.subTexto)
        val comercio = r.comercio ?: "Sin interpretar"
        val sug = if (r.comercio != null) sugerencia(comercio) else null
        val gasto = Gasto(
            fecha = n.momento,
            importeCentimos = r.importeCentimos,
            moneda = r.moneda,
            comercio = comercio,
            tarjeta = r.tarjeta,
            origen = origen,
            claveOrigen = claveOrigen,
            textoCrudo = if (r.interpretado) null else
                listOfNotNull(n.titulo, n.texto, n.textoGrande, n.subTexto).joinToString(" | "),
            interpretado = r.interpretado,
            categoria = sug?.categoria,
            subcategoria = sug?.subcategoria,
        )
        // El mismo pago puede llegar por Wallet y por el banco: se guarda una sola vez.
        val cercanos = dao.entre(gasto.fecha - Duplicados.VENTANA_MAXIMA_MS, gasto.fecha + Duplicados.VENTANA_MAXIMA_MS)
        Duplicados.buscarDeNotificacion(gasto, cercanos)?.let { existente ->
            var mejorado = existente
            if (existente.tarjeta == null && gasto.tarjeta != null) mejorado = mejorado.copy(tarjeta = gasto.tarjeta)
            // El correo suele traer un nombre más claro que el aviso del banco.
            if (existente.origen == Gasto.ORIGEN_SANTANDER && gasto.origen == Gasto.ORIGEN_CORREO && r.comercio != null) {
                mejorado = mejorado.copy(comercio = comercio, categoria = mejorado.categoria ?: gasto.categoria, subcategoria = mejorado.subcategoria ?: gasto.subcategoria)
            }
            if (mejorado != existente) dao.actualizar(mejorado)
            return false
        }
        return dao.insertar(gasto) != -1L
    }

    /** Gasto parecido ya guardado (mismo importe el mismo día), para avisar antes de un alta manual. */
    suspend fun posibleDuplicado(g: Gasto): Gasto? {
        val zona = java.time.ZoneId.systemDefault()
        val dia = java.time.Instant.ofEpochMilli(g.fecha).atZone(zona).toLocalDate()
        val desde = dia.atStartOfDay(zona).toInstant().toEpochMilli()
        val hasta = dia.plusDays(1).atStartOfDay(zona).toInstant().toEpochMilli() - 1
        return Duplicados.buscarParaManual(g, dao.entre(desde, hasta)) { _, _ -> true }
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
            return GastosRepositorio(db.gastoDao(), db.reglaDao(), db.categoriaDao(), db.finanzasDao(), db.recurrenteDao())
        }
    }
}
