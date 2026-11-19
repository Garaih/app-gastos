package com.garaih.appgastos.datos

import java.time.LocalDate
import java.time.YearMonth

/**
 * Periodo de gasto que empieza el día [diaInicio] de cada mes.
 * Con diaInicio = 1 es el mes natural. Con 25, el periodo "de noviembre" va del
 * 25 de octubre al 24 de noviembre. Se identifica por el mes en el que termina.
 */
data class Periodo(val mes: YearMonth, val diaInicio: Int) {

    /** Primer día incluido. */
    val inicio: LocalDate =
        if (diaInicio <= 1) mes.atDay(1) else mes.minusMonths(1).let { it.atDay(minOf(diaInicio, it.lengthOfMonth())) }

    /** Primer día NO incluido. */
    val finExclusivo: LocalDate =
        if (diaInicio <= 1) mes.plusMonths(1).atDay(1) else mes.atDay(minOf(diaInicio, mes.lengthOfMonth()))

    /** Último día incluido. */
    val fin: LocalDate get() = finExclusivo.minusDays(1)

    fun contiene(fecha: LocalDate): Boolean = !fecha.isBefore(inicio) && fecha.isBefore(finExclusivo)

    fun anterior() = Periodo(mes.minusMonths(1), diaInicio)
    fun siguiente() = Periodo(mes.plusMonths(1), diaInicio)

    companion object {
        fun que(fecha: LocalDate, diaInicio: Int): Periodo {
            if (diaInicio <= 1) return Periodo(YearMonth.from(fecha), diaInicio)
            val mes = YearMonth.from(fecha)
            val corte = minOf(diaInicio, mes.lengthOfMonth())
            return if (fecha.dayOfMonth >= corte) Periodo(mes.plusMonths(1), diaInicio) else Periodo(mes, diaInicio)
        }
    }
}
