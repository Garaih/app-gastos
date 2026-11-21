package com.garaih.appgastos.datos

import com.garaih.appgastos.captura.Correo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Remitentes y textos inventados o genéricos. */
class CorreoTest {
    private val lista = Correo.remitentes(Correo.REMITENTES_POR_DEFECTO)

    @Test fun remitenteElegido() = assertEquals("paypal", Correo.remitenteDe("PayPal", lista))
    @Test fun remitenteNoElegidoSeIgnora() = assertNull(Correo.remitenteDe("Amigo Ficticio", lista))
    @Test fun reciboSiPromoNo() {
        assertTrue(Correo.pareceRecibo("Recibo de tu pago de 9,99 € a Servicio Ficticio"))
        assertFalse(Correo.pareceRecibo("Ahorra un 20 % en tu próxima compra"))
        assertFalse(Correo.pareceRecibo("Hola, ¿qué tal todo?"))
    }

    @Test fun correoTrasPagoConTarjetaEsDuplicado() {
        val t = 1_800_000_000_000L
        val banco = Gasto(id = 1, fecha = t, importeCentimos = 999, comercio = "CODIGO RARO", origen = Gasto.ORIGEN_SANTANDER)
        val correo = Gasto(fecha = t + 40 * 60_000, importeCentimos = 999, comercio = "PayPal", origen = Gasto.ORIGEN_CORREO)
        assertEquals(banco, Duplicados.buscarDeNotificacion(correo, listOf(banco)))
    }

    @Test fun reciboDeUnGastoRecurrenteEsDuplicado() {
        val t = 1_800_000_000_000L
        val recurrente = Gasto(id = 2, fecha = t, importeCentimos = 3000, comercio = "Fibra", origen = Gasto.ORIGEN_RECURRENTE)
        val correo = Gasto(fecha = t + 2 * 24 * 3_600_000L, importeCentimos = 3000, comercio = "Operadora", origen = Gasto.ORIGEN_CORREO)
        assertEquals(recurrente, Duplicados.buscarDeNotificacion(correo, listOf(recurrente)))
    }
}
