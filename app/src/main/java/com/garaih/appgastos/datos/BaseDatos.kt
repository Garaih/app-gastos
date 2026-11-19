package com.garaih.appgastos.datos

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Gasto::class, ReglaCategoria::class], version = 2, exportSchema = false)
abstract class BaseDatos : RoomDatabase() {
    abstract fun gastoDao(): GastoDao
    abstract fun reglaDao(): ReglaCategoriaDao

    companion object {
        @Volatile private var instancia: BaseDatos? = null

        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reglas_categoria` " +
                        "(`comercio` TEXT NOT NULL, `categoria` TEXT NOT NULL, PRIMARY KEY(`comercio`))"
                )
            }
        }

        fun obtener(context: Context): BaseDatos =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext, BaseDatos::class.java, "gastos.db"
                ).addMigrations(MIGRACION_1_2).build().also { instancia = it }
            }
    }
}
