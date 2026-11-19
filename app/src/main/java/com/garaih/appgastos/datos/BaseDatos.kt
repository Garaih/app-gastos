package com.garaih.appgastos.datos

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Gasto::class], version = 1, exportSchema = false)
abstract class BaseDatos : RoomDatabase() {
    abstract fun gastoDao(): GastoDao

    companion object {
        @Volatile private var instancia: BaseDatos? = null

        fun obtener(context: Context): BaseDatos =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext, BaseDatos::class.java, "gastos.db"
                ).build().also { instancia = it }
            }
    }
}
