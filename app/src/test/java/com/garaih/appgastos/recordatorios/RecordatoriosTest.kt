package com.garaih.appgastos.recordatorios

import com.garaih.appgastos.datos.Recurrente
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecordatoriosTest {
    private val r = Recurrente(1, "Servicio Ficticio", 1299, dia = 10, inicioMes = 202611, ultimoMes = 202611, recordarDias = 3)

    @Test fun avisaDentroDelMargen() = assertNotNull(Recordatorios.aviso(r, LocalDate.of(2026, 12, 8)))
    @Test fun noAvisaDemasiadoPronto() = assertNull(Recordatorios.aviso(r, LocalDate.of(2026, 12, 1)))
    @Test fun noRepiteElMismoMes() = assertNull(Recordatorios.aviso(r.copy(avisadoMes = 202612), LocalDate.of(2026, 12, 9)))
    @Test fun sinAvisoConfiguradoNoAvisa() = assertNull(Recordatorios.aviso(r.copy(recordarDias = null), LocalDate.of(2026, 12, 9)))
    @Test fun textoDeBaja() {
        val (titulo, _) = Recordatorios.aviso(r.copy(quiereCancelar = true), LocalDate.of(2026, 12, 9))!!
        assertTrue(titulo.startsWith("Da de baja"))
    }
}
