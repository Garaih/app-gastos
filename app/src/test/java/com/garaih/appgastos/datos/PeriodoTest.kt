package com.garaih.appgastos.datos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class PeriodoTest {
    @Test fun del25al24() {
        val p = Periodo.que(LocalDate.of(2026, 11, 19), 25)
        assertEquals(YearMonth.of(2026, 11), p.mes)
        assertEquals(LocalDate.of(2026, 10, 25), p.inicio)
        assertEquals(LocalDate.of(2026, 11, 24), p.fin)
    }

    @Test fun el25EmpiezaElSiguiente() {
        val p = Periodo.que(LocalDate.of(2026, 11, 25), 25)
        assertEquals(YearMonth.of(2026, 12), p.mes)
        assertEquals(LocalDate.of(2026, 11, 25), p.inicio)
        assertTrue(p.contiene(LocalDate.of(2026, 12, 24)))
        assertFalse(p.contiene(LocalDate.of(2026, 12, 25)))
    }

    @Test fun cambioDeAnio() {
        val p = Periodo.que(LocalDate.of(2026, 12, 30), 25)
        assertEquals(YearMonth.of(2027, 1), p.mes)
        assertEquals(LocalDate.of(2027, 1, 24), p.fin)
    }

    @Test fun mesNatural() {
        val p = Periodo.que(LocalDate.of(2026, 2, 10), 1)
        assertEquals(LocalDate.of(2026, 2, 1), p.inicio)
        assertEquals(LocalDate.of(2026, 2, 28), p.fin)
    }

    @Test fun diaQueNoExisteEnFebrero() {
        val p = Periodo(YearMonth.of(2026, 3), 28)
        assertEquals(LocalDate.of(2026, 2, 28), p.inicio)
        assertEquals(LocalDate.of(2026, 3, 27), p.fin)
    }
}
