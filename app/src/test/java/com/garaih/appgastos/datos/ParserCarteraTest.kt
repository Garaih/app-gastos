package com.garaih.appgastos.datos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Todos los ejemplos son inventados. */
class ParserCarteraTest {

    @Test fun formatoEspanolConTarjeta() {
        val r = ParserCartera.interpretar("Panadería Ejemplo", "12,34 € con Visa •••• 4321", null, null)
        assertEquals(1234L, r.importeCentimos)
        assertEquals("Panadería Ejemplo", r.comercio)
        assertEquals("4321", r.tarjeta)
        assertTrue(r.interpretado)
    }

    @Test fun simboloDelanteYPunto() {
        val r = ParserCartera.interpretar("Gasolinera Inventada", "€45.60 with Mastercard ••9876", null, null)
        assertEquals(4560L, r.importeCentimos)
        assertEquals("9876", r.tarjeta)
    }

    @Test fun milesEspanol() {
        assertEquals(123456L, ParserCartera.aCentimos("1.234,56"))
        assertEquals(123456L, ParserCartera.aCentimos("1,234.56"))
        assertEquals(123400L, ParserCartera.aCentimos("1.234"))
        assertEquals(1230L, ParserCartera.aCentimos("12,3"))
        assertEquals(500L, ParserCartera.aCentimos("5"))
    }

    @Test fun eurComoTexto() {
        val r = ParserCartera.interpretar("Tienda Ficticia", "Pago de EUR 7,05", null, null)
        assertEquals(705L, r.importeCentimos)
    }

    @Test fun importeEnElTituloYComercioEnTexto() {
        val r = ParserCartera.interpretar("3,50 €", "Cafetería de Prueba\nTarjeta terminada en 1111", null, null)
        assertEquals(350L, r.importeCentimos)
        assertEquals("Cafetería de Prueba", r.comercio)
        assertEquals("1111", r.tarjeta)
    }

    @Test fun textoGrandeTambienCuenta() {
        val r = ParserCartera.interpretar("Supermercado X", null, "Has pagado 23,99 € con tu tarjeta", null)
        assertEquals(2399L, r.importeCentimos)
        assertNull(r.tarjeta)
    }

    @Test fun sinImporteNoSeInterpreta() {
        val r = ParserCartera.interpretar("Google Wallet", "Tarjeta añadida correctamente", null, null)
        assertNull(r.importeCentimos)
        assertFalse(r.interpretado)
    }
}
