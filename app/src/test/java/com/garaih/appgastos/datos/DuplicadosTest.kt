package com.garaih.appgastos.datos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Pagos inventados. */
class DuplicadosTest {
    private val t = 1_800_000_000_000L
    private val wallet = Gasto(id = 1, fecha = t, importeCentimos = 2350, comercio = "Tienda Ficticia", origen = Gasto.ORIGEN_CARTERA)

    @Test fun mismoPagoPorWalletYBanco() {
        val banco = Gasto(fecha = t + 60_000, importeCentimos = 2350, comercio = "TIENDA FICTICIA", origen = Gasto.ORIGEN_SANTANDER)
        assertEquals(wallet, Duplicados.buscarDeNotificacion(banco, listOf(wallet)))
    }

    @Test fun distintoImporteNoEsDuplicado() {
        val banco = Gasto(fecha = t + 60_000, importeCentimos = 2351, comercio = "x", origen = Gasto.ORIGEN_SANTANDER)
        assertNull(Duplicados.buscarDeNotificacion(banco, listOf(wallet)))
    }

    @Test fun lejosEnElTiempoNoEsDuplicado() {
        val banco = Gasto(fecha = t + 40 * 60_000, importeCentimos = 2350, comercio = "x", origen = Gasto.ORIGEN_SANTANDER)
        assertNull(Duplicados.buscarDeNotificacion(banco, listOf(wallet)))
    }

    @Test fun dosCafesIgualesSeguidosEnLaMismaFuenteSiCuentan() {
        val otro = Gasto(fecha = t + 5 * 60_000, importeCentimos = 2350, comercio = "Tienda Ficticia", origen = Gasto.ORIGEN_CARTERA)
        assertNull(Duplicados.buscarDeNotificacion(otro, listOf(wallet)))
    }

    @Test fun manualNoSeComparaComoAutomatico() {
        val manual = wallet.copy(id = 3, origen = Gasto.ORIGEN_MANUAL)
        val banco = Gasto(fecha = t, importeCentimos = 2350, comercio = "x", origen = Gasto.ORIGEN_SANTANDER)
        assertNull(Duplicados.buscarDeNotificacion(banco, listOf(manual)))
    }
}
