package com.garaih.appgastos.datos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class IngresosFijosTest {
    private fun p(a: Int, m: Int) = Periodo(YearMonth.of(a, m), 25)
    private val historial = listOf(
        IngresoFijo(1, "Nómina", 150000, 202610),
        IngresoFijo(2, "Nómina", 160000, 202612),
    )

    @Test fun antesDelPrimeroNoHayIngreso() = assertTrue(IngresosFijos.vigentes(historial, p(2026, 9)).isEmpty())
    @Test fun seRepiteCadaPeriodo() = assertEquals(150000L, IngresosFijos.vigentes(historial, p(2026, 11)).single().importeCentimos)
    @Test fun elCambioQuedaPorDefecto() {
        assertEquals(160000L, IngresosFijos.vigentes(historial, p(2026, 12)).single().importeCentimos)
        assertEquals(160000L, IngresosFijos.vigentes(historial, p(2027, 5)).single().importeCentimos)
    }
    @Test fun ceroEsDejarDeCobrar() {
        val h = historial + IngresoFijo(3, "Nómina", 0, 202703)
        assertTrue(IngresosFijos.vigentes(h, p(2027, 3)).isEmpty())
        assertEquals(160000L, IngresosFijos.vigentes(h, p(2027, 2)).single().importeCentimos)
    }
}
