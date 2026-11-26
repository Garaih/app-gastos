package com.garaih.appgastos.datos

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

/**
 * Gasto que se repite cada mes el día [dia]. Si [plazos] no es null, se repite solo ese número
 * de veces (financiaciones). Los meses se guardan como AAAAMM (mes natural del cargo).
 * [ultimoMes] es el último mes ya apuntado como Gasto: así, si borras uno, no vuelve a aparecer.
 */
@Entity(tableName = "recurrentes")
data class Recurrente(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val comercio: String,
    val importeCentimos: Long,
    val categoria: String? = null,
    val subcategoria: String? = null,
    val dia: Int,
    val inicioMes: Int,
    val plazos: Int? = null,
    val ultimoMes: Int,
    val activo: Boolean = true,
    val creado: Long = System.currentTimeMillis(),
    /** Avisar N días antes de cada cargo (null = sin aviso). */
    val recordarDias: Int? = null,
    /** El usuario quiere darlo de baja: el aviso le recuerda cancelarlo antes del cargo. */
    @ColumnInfo(defaultValue = "0") val quiereCancelar: Boolean = false,
    /** Último mes (AAAAMM) del que ya se avisó, para no repetir el recordatorio. */
    val avisadoMes: Int? = null,
)

@Dao
interface RecurrenteDao {
    @Query("SELECT * FROM recurrentes ORDER BY activo DESC, dia") fun observarTodos(): Flow<List<Recurrente>>
    @Query("SELECT * FROM recurrentes WHERE activo = 1") suspend fun activos(): List<Recurrente>
    @Query("SELECT * FROM recurrentes WHERE id = :id") suspend fun porId(id: Long): Recurrente?
    @Insert suspend fun insertar(r: Recurrente): Long
    @Update suspend fun actualizar(r: Recurrente)
    @Delete suspend fun borrar(r: Recurrente)
}

object Recurrencia {
    fun aClave(m: YearMonth): Int = m.year * 100 + m.monthValue
    fun aMes(clave: Int): YearMonth = YearMonth.of(clave / 100, clave % 100)

    /** Número de plazo (1, 2, ...) que corresponde a un mes. */
    fun numeroPlazo(r: Recurrente, mes: YearMonth): Int =
        (mes.year * 12 + mes.monthValue) - (aMes(r.inicioMes).let { it.year * 12 + it.monthValue }) + 1

    fun fechaCargo(r: Recurrente, mes: YearMonth): LocalDate = mes.atDay(minOf(r.dia, mes.lengthOfMonth()))

    data class Cargo(val mes: YearMonth, val plazo: Int, val fecha: LocalDate)

    /** Cargos pendientes de apuntar hasta hoy (incluido). */
    fun pendientes(r: Recurrente, hoy: LocalDate): List<Cargo> {
        if (!r.activo) return emptyList()
        val lista = mutableListOf<Cargo>()
        var mes = aMes(r.ultimoMes).plusMonths(1)
        while (true) {
            val n = numeroPlazo(r, mes)
            if (r.plazos != null && n > r.plazos) break
            val fecha = fechaCargo(r, mes)
            if (fecha.isAfter(hoy)) break
            lista += Cargo(mes, n, fecha)
            mes = mes.plusMonths(1)
        }
        return lista
    }

    /** Siguiente cargo aún no apuntado, o null si ya terminó. */
    fun siguiente(r: Recurrente): Cargo? {
        if (!r.activo) return null
        val mes = aMes(r.ultimoMes).plusMonths(1)
        val n = numeroPlazo(r, mes)
        if (r.plazos != null && n > r.plazos) return null
        return Cargo(mes, n, fechaCargo(r, mes))
    }

    /** Plazos que faltan por pagar después del último apuntado (null = sin fin). */
    fun plazosRestantes(r: Recurrente): Int? =
        r.plazos?.let { (it - numeroPlazo(r, aMes(r.ultimoMes))).coerceAtLeast(0) }

    fun terminado(r: Recurrente): Boolean = r.plazos != null && plazosRestantes(r) == 0
}
