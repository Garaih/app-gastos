package com.garaih.appgastos.datos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Comercios inventados o genéricos. */
class ClasificadorTest {
    private val cats = CategoriasIniciales.lista
    private fun sug(c: String) = Clasificador.sugerir(c, cats)

    @Test fun gasolinera() {
        assertEquals("coche", sug("Estación de Servicio Ejemplo")?.categoria)
        assertEquals("Gasolina", sug("Estación de Servicio Ejemplo")?.subcategoria)
    }
    @Test fun peaje() = assertEquals(Clasificador.Sugerencia("coche", "Peajes"), sug("AUTOPISTA DEL NORTE PEAJE"))
    @Test fun supermercado() = assertEquals(Clasificador.Sugerencia("super", null), sug("Supermercado de Barrio"))
    @Test fun online() = assertEquals("online", sug("AMZN Mktp ES")?.categoria)
    @Test fun acentos() = assertEquals(Clasificador.Sugerencia("restaurantes", "Cafeterías"), sug("Cafetería Inventada"))
    @Test fun barNoCaseDentroDePalabra() = assertNull(sug("Barbería Ficticia"))
    @Test fun barComoPalabra() = assertEquals("restaurantes", sug("Bar El Ejemplo")?.categoria)
    @Test fun gasNoCaseConGasolinera() = assertEquals("coche", sug("Gasolinera Ficticia")?.categoria)
    @Test fun desconocido() = assertNull(sug("Comercio Desconocido"))
    @Test fun normaliza() = assertEquals("cafe la plaza", Clasificador.normalizar("  Café   La Plaza "))

    @Test fun subcategoriasIdaYVuelta() {
        val lista = listOf(Subcategoria("Gasolina", listOf("a", "b c")), Subcategoria("Seguro", emptyList()))
        assertEquals(lista, Categoria.decodificarSubcategorias(Categoria.codificarSubcategorias(lista)))
    }
}
