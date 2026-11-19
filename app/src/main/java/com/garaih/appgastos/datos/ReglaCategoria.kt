package com.garaih.appgastos.datos

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/** Categoría que el usuario eligió para un comercio; se aplica a sus gastos futuros. */
@Entity(tableName = "reglas_categoria")
data class ReglaCategoria(
    @PrimaryKey val comercio: String,
    val categoria: String,
)

@Dao
interface ReglaCategoriaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(regla: ReglaCategoria)

    @Query("SELECT * FROM reglas_categoria WHERE comercio = :comercio LIMIT 1")
    suspend fun buscar(comercio: String): ReglaCategoria?
}
