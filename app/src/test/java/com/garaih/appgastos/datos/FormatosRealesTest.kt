package com.garaih.appgastos.datos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Formatos tal como llegan en el móvil (noviembre de 2026), con comercios, importes y
 * tarjetas inventados.
 */
class FormatosRealesTest {

    @Test fun walletEuros() {
        val r = ParserCartera.interpretar("BAR DE PRUEBA", "8,50 € con DEBITO BANCO ••1111", null, null)
        assertEquals(850L, r.importeCentimos)
        assertEquals("BAR DE PRUEBA", r.comercio)
        assertEquals("1111", r.tarjeta)
    }

    @Test fun walletDolares() {
        val r = ParserCartera.interpretar("SERVICIO EXTRANJERO", "20,00 US$ con DEBITO BANCO ••1111", null, null)
        assertNull(r.importeCentimos)
        assertEquals("USD", r.moneda)
        assertEquals("20,00 USD", r.importeExtranjero)
        assertEquals("SERVICIO EXTRANJERO", r.comercio)
        assertEquals("1111", r.tarjeta)
    }

    @Test fun santanderPagoSinComercio() {
        val r = ParserCartera.interpretar(
            "✅ ¡Has hecho un pago con tarjeta!",
            "Has pagado 8.50 EUR en  con tu tarjeta **1111. Puedes entrar en tu App para ver el detalle.",
            null, null,
        )
        assertEquals(850L, r.importeCentimos)
        assertNull(r.comercio)
        assertEquals("1111", r.tarjeta)
    }

    @Test fun santanderRetencion() {
        val r = ParserCartera.interpretar(
            "⏸️ Retención en tu tarjeta",
            "Se ha realizado una retención de 18.37 EUR en tu tarjeta **1111.",
            null, null,
        )
        assertEquals(1837L, r.importeCentimos)
        assertEquals("1111", r.tarjeta)
    }

    @Test fun walletYSantanderMismoPagoSeUnen() {
        val t = 1_800_000_000_000L
        val santander = Gasto(id = 1, fecha = t, importeCentimos = 850, comercio = Duplicados.SIN_COMERCIO,
            tarjeta = "1111", origen = Gasto.ORIGEN_SANTANDER)
        val wallet = Gasto(fecha = t + 20_000, importeCentimos = 850, comercio = "BAR DE PRUEBA",
            tarjeta = "1111", origen = Gasto.ORIGEN_CARTERA)
        val existente = Duplicados.buscarDeNotificacion(wallet, listOf(santander))!!
        assertEquals("BAR DE PRUEBA", Duplicados.unir(existente, wallet).comercio)
    }

    @Test fun pagoEnDolaresSeCompletaConLaRetencion() {
        val t = 1_800_000_000_000L
        val wallet = Gasto(id = 1, fecha = t, importeCentimos = null, moneda = "USD", comercio = "SERVICIO EXTRANJERO",
            tarjeta = "1111", origen = Gasto.ORIGEN_CARTERA, interpretado = false, textoCrudo = "x")
        val retencion = Gasto(fecha = t + 30_000, importeCentimos = 1837, comercio = Duplicados.SIN_COMERCIO,
            tarjeta = "1111", origen = Gasto.ORIGEN_SANTANDER)
        assertNull(Duplicados.buscarDeNotificacion(retencion, listOf(wallet)))
        val existente = Duplicados.buscarPorTarjeta(retencion, listOf(wallet))!!
        val unido = Duplicados.unir(existente, retencion)
        assertEquals(1837L, unido.importeCentimos)
        assertEquals("SERVICIO EXTRANJERO", unido.comercio)
        assertEquals(true, unido.interpretado)
    }
}
