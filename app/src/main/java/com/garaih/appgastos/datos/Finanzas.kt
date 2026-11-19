package com.garaih.appgastos.datos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Límite de gasto por periodo para una categoría. */
@Entity(tableName = "presupuestos")
data class Presupuesto(
    @PrimaryKey val categoria: String,
    val limiteCentimos: Long,
)

/** Objetivo de ahorro con lo apartado hasta ahora. */
@Entity(tableName = "objetivos")
data class Objetivo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val emoji: String,
    val objetivoCentimos: Long,
    val ahorradoCentimos: Long = 0,
    val fechaLimite: Long? = null,
    val creado: Long = System.currentTimeMillis(),
)

/** Inversión con seguimiento manual del valor (la app no tiene conexión a internet). */
@Entity(tableName = "inversiones")
data class Inversion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val tipo: String,
    val invertidoCentimos: Long,
    val valorCentimos: Long,
    val actualizado: Long = System.currentTimeMillis(),
) {
    companion object {
        val TIPOS = listOf("Fondo indexado", "ETF", "Acciones", "Plan de pensiones", "Depósito", "Cripto", "Otro")
    }
}

/** Ingreso (nómina, extra...). */
@Entity(tableName = "ingresos")
data class Ingreso(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: Long,
    val importeCentimos: Long,
    val concepto: String,
)

@Dao
interface FinanzasDao {
    @Query("SELECT * FROM presupuestos") fun presupuestos(): Flow<List<Presupuesto>>
    @Query("SELECT * FROM presupuestos WHERE categoria = :categoria") suspend fun presupuesto(categoria: String): Presupuesto?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun guardarPresupuesto(p: Presupuesto)
    @Query("DELETE FROM presupuestos WHERE categoria = :categoria") suspend fun borrarPresupuesto(categoria: String)

    @Query("SELECT * FROM objetivos ORDER BY creado") fun objetivos(): Flow<List<Objetivo>>
    @Insert suspend fun insertarObjetivo(o: Objetivo): Long
    @Update suspend fun actualizarObjetivo(o: Objetivo)
    @Delete suspend fun borrarObjetivo(o: Objetivo)

    @Query("SELECT * FROM inversiones ORDER BY nombre") fun inversiones(): Flow<List<Inversion>>
    @Insert suspend fun insertarInversion(i: Inversion): Long
    @Update suspend fun actualizarInversion(i: Inversion)
    @Delete suspend fun borrarInversion(i: Inversion)

    @Query("SELECT * FROM ingresos ORDER BY fecha DESC") fun ingresos(): Flow<List<Ingreso>>
    @Insert suspend fun insertarIngreso(i: Ingreso): Long
    @Update suspend fun actualizarIngreso(i: Ingreso)
    @Delete suspend fun borrarIngreso(i: Ingreso)
}

fun Inversion.ganancia(): Long = valorCentimos - invertidoCentimos
fun Inversion.rentabilidad(): Double = if (invertidoCentimos > 0) ganancia().toDouble() / invertidoCentimos else 0.0
