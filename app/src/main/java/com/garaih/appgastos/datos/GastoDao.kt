package com.garaih.appgastos.datos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GastoDao {
    /** Devuelve -1 si ya existía un gasto con la misma claveOrigen (duplicado). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(gasto: Gasto): Long

    @Update
    suspend fun actualizar(gasto: Gasto)

    @Delete
    suspend fun borrar(gasto: Gasto)

    @Query("DELETE FROM gastos WHERE origen = :origen")
    suspend fun borrarPorOrigen(origen: String)

    @Query("SELECT * FROM gastos ORDER BY fecha DESC")
    fun observarTodos(): Flow<List<Gasto>>

    @Query("SELECT * FROM gastos WHERE categoria IS NULL")
    suspend fun sinCategoria(): List<Gasto>
}
