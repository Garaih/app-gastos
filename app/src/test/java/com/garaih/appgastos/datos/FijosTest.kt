package com.garaih.appgastos.datos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

/** Datos inventados. */
class FijosTest {
    private val utc = ZoneOffset.UTC
    private fun ms(a: Int, m: Int, d: Int) = LocalDate.of(a, m, d).atTime(10, 0).toInstant(utc).toEpochMilli()

    // ---- Duplicados entre cargo recurrente y aviso ----

    @Test fun avisoDespuesDelRecurrenteSeUne() {
        val rec = Gasto(id = 1, fecha = ms(2026, 11, 5), importeCentimos = 1299, comercio = "Plataforma Vídeo", origen = Gasto.ORIGEN_RECURRENTE, recurrenteId = 7)
        val aviso = Gasto(fecha = ms(2026, 11, 6), importeCentimos = 1299, comercio = "PLATAFORMA VIDEO", origen = Gasto.ORIGEN_CARTERA)
        val e = Duplicados.buscarDeNotificacion(aviso, listOf(rec))!!
        assertEquals(1L, e.id)
        assertEquals(ms(2026, 11, 6), Duplicados.unir(e, aviso).fecha)
    }

    @Test fun subidaDePrecioMismoComercioSeUneYMandaElImporteReal() {
        val rec = Gasto(id = 1, fecha = ms(2026, 11, 5), importeCentimos = 1299, comercio = "Plataforma Vídeo", origen = Gasto.ORIGEN_RECURRENTE, recurrenteId = 7)
        val aviso = Gasto(fecha = ms(2026, 11, 5), importeCentimos = 1399, comercio = "PLATAFORMA VIDEO", origen = Gasto.ORIGEN_SANTANDER)
        val e = Duplicados.buscarDeNotificacion(aviso, listOf(rec))!!
        assertEquals(1399L, Duplicados.unir(e, aviso).importeCentimos)
    }

    @Test fun otroComercioOtroImporteNoSeUne() {
        val rec = Gasto(id = 1, fecha = ms(2026, 11, 5), importeCentimos = 1299, comercio = "Plataforma Vídeo", origen = Gasto.ORIGEN_RECURRENTE)
        val aviso = Gasto(fecha = ms(2026, 11, 5), importeCentimos = 4500, comercio = "Gasolinera Ficticia", origen = Gasto.ORIGEN_CARTERA)
        assertNull(Duplicados.buscarDeNotificacion(aviso, listOf(rec)))
    }

    @Test fun avisoAntesDelRecurrenteSeVincula() {
        val aviso = Gasto(id = 3, fecha = ms(2026, 11, 4), importeCentimos = 3000, comercio = "OPERADORA FIBRA", origen = Gasto.ORIGEN_CORREO)
        val cargo = Gasto(fecha = ms(2026, 11, 5), importeCentimos = 3000, comercio = "Internet casa", origen = Gasto.ORIGEN_RECURRENTE, recurrenteId = 9)
        assertEquals(3L, Duplicados.buscarAvisoParaRecurrente(cargo, listOf(aviso))?.id)
    }

    @Test fun avisoYaVinculadoNoSeVuelveAUsar() {
        val aviso = Gasto(id = 3, fecha = ms(2026, 11, 4), importeCentimos = 3000, comercio = "x", origen = Gasto.ORIGEN_CORREO, recurrenteId = 9)
        val cargo = Gasto(fecha = ms(2026, 12, 5), importeCentimos = 3000, comercio = "Internet casa", origen = Gasto.ORIGEN_RECURRENTE, recurrenteId = 9)
        assertNull(Duplicados.buscarAvisoParaRecurrente(cargo, listOf(aviso)))
    }

    // ---- Detección de suscripciones ----

    @Test fun detectaPagoMensualParecido() {
        val g = listOf(
            Gasto(id = 1, fecha = ms(2026, 9, 3), importeCentimos = 999, comercio = "Música Ficticia", origen = Gasto.ORIGEN_CARTERA),
            Gasto(id = 2, fecha = ms(2026, 10, 3), importeCentimos = 999, comercio = "MUSICA FICTICIA", origen = Gasto.ORIGEN_CARTERA),
            Gasto(id = 3, fecha = ms(2026, 11, 3), importeCentimos = 1099, comercio = "Música Ficticia", origen = Gasto.ORIGEN_SANTANDER),
            Gasto(id = 4, fecha = ms(2026, 11, 8), importeCentimos = 2500, comercio = "Tienda Única", origen = Gasto.ORIGEN_CARTERA),
        )
        val s = Suscripciones.detectar(g, emptyList(), YearMonth.of(2026, 11), utc)
        assertEquals(1, s.size)
        assertEquals(3, s.single().meses)
        assertEquals(1099L, s.single().importeCentimos)
    }

    @Test fun noSugiereLoQueYaEsRecurrente() {
        val g = listOf(
            Gasto(id = 1, fecha = ms(2026, 10, 3), importeCentimos = 999, comercio = "Música Ficticia", origen = Gasto.ORIGEN_CARTERA),
            Gasto(id = 2, fecha = ms(2026, 11, 3), importeCentimos = 999, comercio = "Música Ficticia", origen = Gasto.ORIGEN_CARTERA),
        )
        val r = Recurrente(1, "Música Ficticia", 999, dia = 3, inicioMes = 202610, ultimoMes = 202611)
        assertTrue(Suscripciones.detectar(g, listOf(r), YearMonth.of(2026, 11), utc).isEmpty())
    }

    // ---- Saldo acumulado ----

    @Test fun loQueSobraPasaAlSiguientePeriodo() {
        val fecha = { m: Long -> java.time.Instant.ofEpochMilli(m).atZone(utc).toLocalDate() }
        val fijos = listOf(IngresoFijo(1, "Nómina", 100000, 202610))
        val gastos = listOf(
            Gasto(fecha = ms(2026, 10, 1), importeCentimos = 70000, comercio = "a", origen = Gasto.ORIGEN_MANUAL), // periodo oct: sobran 300
            Gasto(fecha = ms(2026, 11, 1), importeCentimos = 120000, comercio = "b", origen = Gasto.ORIGEN_MANUAL), // periodo nov: faltan 200
        )
        val inicio = Saldo.inicioAutomatico(25, emptyList(), fijos, fecha)
        assertNotNull(inicio)
        val dic = Saldo.calcular(Periodo(YearMonth.of(2026, 12), 25), inicio, 0, gastos, emptyList(), fijos, fecha)
        assertEquals(30000L - 20000L, dic.arrastre)
        assertEquals(10000L + 100000L, dic.disponible)
    }

    @Test fun saldoInicialSeSuma() {
        val fecha = { m: Long -> java.time.Instant.ofEpochMilli(m).atZone(utc).toLocalDate() }
        val r = Saldo.calcular(Periodo(YearMonth.of(2026, 11), 25), Periodo(YearMonth.of(2026, 11), 25), 50000, emptyList(), emptyList(), emptyList(), fecha)
        assertEquals(50000L, r.disponible)
    }
}
