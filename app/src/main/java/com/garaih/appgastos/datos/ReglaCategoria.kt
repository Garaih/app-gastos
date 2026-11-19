package com.garaih.appgastos.datos

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/** Categoría (y subcategoría) que el usuario eligió para un comercio; se aplica a sus gastos futuros. */
@Entity(tableName = "reglas_categoria")
data class ReglaCategoria(
    @PrimaryKey val comercio: String,
    val categoria: String,
    val subcategoria: String? = null,
)

@Dao
interface ReglaCategoriaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(regla: ReglaCategoria)

    @Query("SELECT * FROM reglas_categoria WHERE comercio = :comercio LIMIT 1")
    suspend fun buscar(comercio: String): ReglaCategoria?

    @Query("DELETE FROM reglas_categoria WHERE categoria = :categoria")
    suspend fun borrarDeCategoria(categoria: String)
}
