package com.garaih.appgastos.datos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecurrenciaTest {
    private val netflix = Recurrente(1, "Suscripción", 1299, dia = 5, inicioMes = 202609, ultimoMes = 202609)
    private val plazos = Recurrente(2, "Compra a plazos", 5000, dia = 20, inicioMes = 202610, plazos = 3, ultimoMes = 202610)

    @Test fun sinFinApuntaCadaMesHastaHoy() {
        val p = Recurrencia.pendientes(netflix, LocalDate.of(2026, 11, 19))
        assertEquals(listOf(2, 3), p.map { it.plazo })
        assertEquals(LocalDate.of(2026, 11, 5), p.last().fecha)
    }

    @Test fun noApuntaCargosFuturos() {
        val p = Recurrencia.pendientes(plazos, LocalDate.of(2026, 11, 19))
        assertTrue(p.isEmpty())
        assertEquals(2, Recurrencia.siguiente(plazos)?.plazo)
    }

    @Test fun aPlazosParaAlFinal() {
        val p = Recurrencia.pendientes(plazos, LocalDate.of(2027, 6, 1))
        assertEquals(listOf(2, 3), p.map { it.plazo })
        val tras = plazos.copy(ultimoMes = Recurrencia.aClave(p.last().mes))
        assertTrue(Recurrencia.terminado(tras))
        assertNull(Recurrencia.siguiente(tras))
        assertEquals(0, Recurrencia.plazosRestantes(tras))
    }

    @Test fun plazosRestantes() = assertEquals(2, Recurrencia.plazosRestantes(plazos))

    @Test fun dia31EnFebrero() {
        val r = netflix.copy(dia = 28)
        assertEquals(LocalDate.of(2027, 2, 28), Recurrencia.fechaCargo(r, java.time.YearMonth.of(2027, 2)))
    }
}
