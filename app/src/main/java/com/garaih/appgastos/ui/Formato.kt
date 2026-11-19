package com.garaih.appgastos.ui

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val LOCALE_ES: Locale = Locale.forLanguageTag("es-ES")

private val formatoEuros: NumberFormat = NumberFormat.getCurrencyInstance(LOCALE_ES)
private val formatoDia = DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", LOCALE_ES)
private val formatoFechaCorta = DateTimeFormatter.ofPattern("d MMM yyyy", LOCALE_ES)

fun euros(centimos: Long): String = formatoEuros.format(centimos / 100.0)

fun aFechaLocal(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

fun fechaHora(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(formatoDia).mayuscula()

fun fechaCorta(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(formatoFechaCorta)

fun nombreMes(mes: YearMonth): String =
    mes.month.getDisplayName(TextStyle.FULL_STANDALONE, LOCALE_ES).mayuscula() + " " + mes.year

fun nombreMesCorto(mes: YearMonth): String =
    mes.month.getDisplayName(TextStyle.FULL_STANDALONE, LOCALE_ES)

/** "12,34" -> "12,34" para editar; null -> "". */
fun importeEditable(centimos: Long?): String =
    centimos?.let { "%d,%02d".format(it / 100, it % 100) } ?: ""

fun String.mayuscula(): String = replaceFirstChar { it.titlecase(LOCALE_ES) }
