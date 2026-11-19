package com.garaih.appgastos.datos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Comercios inventados o genéricos. */
class ClasificadorTest {
    @Test fun gasolinera() = assertEquals(Categoria.COCHE, Clasificador.sugerir("Estación de Servicio Ejemplo"))
    @Test fun peaje() = assertEquals(Categoria.COCHE, Clasificador.sugerir("AUTOPISTA DEL NORTE PEAJE"))
    @Test fun supermercado() = assertEquals(Categoria.SUPERMERCADO, Clasificador.sugerir("Supermercado de Barrio"))
    @Test fun online() = assertEquals(Categoria.ONLINE, Clasificador.sugerir("AMZN Mktp ES"))
    @Test fun acentos() = assertEquals(Categoria.RESTAURANTES, Clasificador.sugerir("Cafetería Inventada"))
    @Test fun barNoCaseDentroDePalabra() = assertNull(Clasificador.sugerir("Barbería Ficticia"))
    @Test fun desconocido() = assertNull(Clasificador.sugerir("Comercio Desconocido"))
    @Test fun normaliza() = assertEquals("cafe la plaza", Clasificador.normalizar("  Café   La Plaza "))
}
