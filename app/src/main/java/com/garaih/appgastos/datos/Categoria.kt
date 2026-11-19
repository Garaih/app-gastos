package com.garaih.appgastos.datos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Subcategoría con sus palabras clave para clasificar automáticamente. */
data class Subcategoria(val nombre: String, val palabras: List<String>)

/**
 * Categoría editable por el usuario.
 * subcategorias: una por línea, "Nombre=palabra1,palabra2".
 * palabrasClave: separadas por comas (además de las de las subcategorías).
 */
@Entity(tableName = "categorias")
data class Categoria(
    @PrimaryKey val clave: String,
    val nombre: String,
    val emoji: String,
    val color: Long,
    val orden: Int,
    val subcategorias: String = "",
    val palabrasClave: String = "",
) {
    fun listaSubcategorias(): List<Subcategoria> = decodificarSubcategorias(subcategorias)
    fun listaPalabras(): List<String> = decodificarPalabras(palabrasClave)

    companion object {
        fun decodificarPalabras(texto: String): List<String> =
            texto.split(',').map { it.trim() }.filter { it.isNotEmpty() }

        fun decodificarSubcategorias(texto: String): List<Subcategoria> =
            texto.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { linea ->
                val nombre = linea.substringBefore('=').trim()
                val palabras = if ('=' in linea) decodificarPalabras(linea.substringAfter('=')) else emptyList()
                Subcategoria(nombre, palabras)
            }.filter { it.nombre.isNotEmpty() }

        fun codificarSubcategorias(lista: List<Subcategoria>): String =
            lista.filter { it.nombre.isNotBlank() }.joinToString("\n") { s ->
                s.nombre.replace("=", "").replace("\n", " ").trim() + "=" +
                    s.palabras.joinToString(",") { it.replace(",", "").trim() }
            }
    }
}

@Dao
interface CategoriaDao {
    @Query("SELECT * FROM categorias ORDER BY orden, nombre")
    fun observarTodas(): Flow<List<Categoria>>

    @Query("SELECT * FROM categorias ORDER BY orden, nombre")
    suspend fun todas(): List<Categoria>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(categoria: Categoria)

    @Delete
    suspend fun borrar(categoria: Categoria)

    @Query("SELECT COALESCE(MAX(orden), 0) FROM categorias")
    suspend fun ordenMaximo(): Int
}

/** Categorías iniciales. Las claves coinciden con las de la versión 0.2. */
object CategoriasIniciales {
    val lista: List<Categoria> = listOf(
        Categoria(
            "coche", "Coche", "🚗", 0xFF3B82F6, 1,
            subcategorias = """
                Gasolina=gasolinera,repsol,cepsa,moeve,galp,shell,petronor,ballenoil,plenoil,petroprix,bp,carburante,combustible,estacion de servicio,e.s.
                Peajes=peaje,peajes,autopista,audasa,autoestradas,ap-9,ap9
                Taller=taller,neumaticos,midas,norauto,feu vert,recambios
                Parking=parking,aparcamiento
                Lavado=lavado,lavadero
                ITV=itv
                Seguro=
            """.trimIndent(),
        ),
        Categoria(
            "super", "Supermercados", "🛒", 0xFF22A55E, 2,
            palabrasClave = "mercadona,gadis,froiz,eroski,carrefour,lidl,aldi,dia,alcampo,familia,hipercor,supermercado,supermercados,super,consum,vegalsa,claudio",
        ),
        Categoria(
            "casa", "Casa", "🏠", 0xFFF59E0B, 3,
            subcategorias = """
                Luz=iberdrola,endesa,naturgy,fenosa,holaluz
                Agua=agua,aguas
                Internet y móvil=movistar,vodafone,orange,digi,jazztel,pepephone,simyo,lowi,r cable
                Gas=butano,gas natural
                Bricolaje=leroy merlin,bricomart,brico depot,ferreteria,bauhaus
                Muebles y hogar=ikea,jysk,conforama,zara home,maisons du monde
                Electrodomésticos=mediamarkt,worten
                Alquiler o hipoteca=
            """.trimIndent(),
        ),
        Categoria(
            "restaurantes", "Restaurantes y bares", "🍽️", 0xFFEF4444, 4,
            subcategorias = """
                A domicilio=just eat,glovo,uber eats,telepizza
                Cafeterías=cafe,cafeteria,churreria,pasteleria,panaderia
            """.trimIndent(),
            palabrasClave = "restaurante,bar,pizzeria,burger,mcdonald's,mcdonalds,kfc,taberna,meson,pulperia,tapas",
        ),
        Categoria(
            "ocio", "Ocio", "🎮", 0xFFA855F7, 5,
            subcategorias = """
                Suscripciones=netflix,spotify,hbo,max,disney,prime video,twitch,youtube
                Videojuegos=steam,playstation,nintendo,xbox,epic games,battle.net,blizzard
                Cine y eventos=cine,cines,teatro,concierto,entradas,ticketmaster
                Deporte=gimnasio,decathlon
            """.trimIndent(),
        ),
        Categoria(
            "online", "Compras online", "📦", 0xFF06B6D4, 6,
            palabrasClave = "amazon,amzn,aliexpress,shein,temu,zalando,pccomponentes,ebay,wallapop,vinted",
        ),
        Categoria(
            "salud", "Salud", "💊", 0xFFEC4899, 7,
            subcategorias = """
                Farmacia=farmacia
                Médico=clinica,dentista,hospital,fisioterapia
                Óptica=optica
                Veterinario=veterinario,veterinaria
            """.trimIndent(),
        ),
        Categoria("otros", "Otros", "🧾", 0xFF64748B, 8),
    )
}
