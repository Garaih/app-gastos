package com.garaih.appgastos.datos

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Un gasto. El importe va en céntimos para evitar errores de redondeo.
 * importeCentimos es null si la notificación no se pudo interpretar.
 */
@Entity(
    tableName = "gastos",
    indices = [Index(value = ["claveOrigen"], unique = true)]
)
data class Gasto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: Long,
    val importeCentimos: Long?,
    val moneda: String = "EUR",
    val comercio: String,
    /** Solo los 4 últimos dígitos, si la notificación los muestra. */
    val tarjeta: String? = null,
    val origen: String,
    /** Identifica la notificación de origen para no duplicar. Null en gastos manuales. */
    val claveOrigen: String? = null,
    /** Texto original, solo si no se pudo interpretar (para revisarlo a mano). */
    val textoCrudo: String? = null,
    val interpretado: Boolean = true,
    /** Reservado para la interfaz con categorías (Coche, Supermercados, Casa...). */
    val categoria: String? = null,
) {
    companion object {
        const val ORIGEN_CARTERA = "cartera"
        const val ORIGEN_MANUAL = "manual"
        const val ORIGEN_PRUEBA = "prueba"
    }
}
