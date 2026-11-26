package com.garaih.appgastos.datos

/**
 * Visión global: lo que sobra (o falta) de cada periodo pasa al siguiente.
 *
 * Arrastre al empezar un periodo = saldo inicial + Σ (ingresos − gastos) de todos los periodos
 * anteriores desde [inicio]. Lo apartado en objetivos de ahorro se resta aparte.
 */
object Saldo {

    data class Resumen(
        val inicio: Periodo,
        val arrastre: Long,
        val ingresos: Long,
        val gastos: Long,
    ) {
        /** Lo disponible al final de este periodo, contando lo que venía de antes. */
        val disponible: Long get() = arrastre + ingresos - gastos
        val delPeriodo: Long get() = ingresos - gastos
    }

    fun ingresosDe(p: Periodo, ingresos: List<Ingreso>, fijos: List<IngresoFijo>, fecha: (Long) -> java.time.LocalDate): Long =
        ingresos.filter { p.contiene(fecha(it.fecha)) }.sumOf { it.importeCentimos } +
            IngresosFijos.vigentes(fijos, p).sumOf { it.importeCentimos }

    fun gastosDe(p: Periodo, gastos: List<Gasto>, fecha: (Long) -> java.time.LocalDate): Long =
        gastos.filter { p.contiene(fecha(it.fecha)) }.sumOf { it.importeCentimos ?: 0L }

    /**
     * Primer periodo desde el que se cuenta: el elegido por el usuario o, si no, el primero con
     * algún ingreso (así no cuentan en negativo los gastos de antes de empezar a usar la app).
     */
    fun inicioAutomatico(diaInicio: Int, ingresos: List<Ingreso>, fijos: List<IngresoFijo>, fecha: (Long) -> java.time.LocalDate): Periodo? {
        val deFijos = fijos.filter { it.importeCentimos > 0 }.minOfOrNull { it.desdeMes }
            ?.let { Periodo(java.time.YearMonth.of(it / 100, it % 100), diaInicio) }
        val dePuntuales = ingresos.minByOrNull { it.fecha }?.let { Periodo.que(fecha(it.fecha), diaInicio) }
        return listOfNotNull(deFijos, dePuntuales).minByOrNull { it.mes }
    }

    fun calcular(
        periodo: Periodo,
        inicio: Periodo?,
        saldoInicial: Long,
        gastos: List<Gasto>,
        ingresos: List<Ingreso>,
        fijos: List<IngresoFijo>,
        fecha: (Long) -> java.time.LocalDate,
    ): Resumen {
        val desde = inicio ?: periodo
        var arrastre = saldoInicial
        var p = desde
        var vueltas = 0
        while (p.mes < periodo.mes && vueltas < 600) {
            arrastre += ingresosDe(p, ingresos, fijos, fecha) - gastosDe(p, gastos, fecha)
            p = p.siguiente()
            vueltas++
        }
        return Resumen(
            inicio = desde,
            arrastre = if (periodo.mes < desde.mes) 0 else arrastre,
            ingresos = ingresosDe(periodo, ingresos, fijos, fecha),
            gastos = gastosDe(periodo, gastos, fecha),
        )
    }
}
