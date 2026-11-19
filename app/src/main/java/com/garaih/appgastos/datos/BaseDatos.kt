package com.garaih.appgastos.datos

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Gasto::class, ReglaCategoria::class, Categoria::class],
    version = 3,
    exportSchema = false,
)
abstract class BaseDatos : RoomDatabase() {
    abstract fun gastoDao(): GastoDao
    abstract fun reglaDao(): ReglaCategoriaDao
    abstract fun categoriaDao(): CategoriaDao

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

        private val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `gastos` ADD COLUMN `subcategoria` TEXT")
                db.execSQL("ALTER TABLE `reglas_categoria` ADD COLUMN `subcategoria` TEXT")
                crearTablaCategorias(db)
                sembrarCategorias(db)
            }
        }

        private fun crearTablaCategorias(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `categorias` (`clave` TEXT NOT NULL, `nombre` TEXT NOT NULL, " +
                    "`emoji` TEXT NOT NULL, `color` INTEGER NOT NULL, `orden` INTEGER NOT NULL, " +
                    "`subcategorias` TEXT NOT NULL, `palabrasClave` TEXT NOT NULL, PRIMARY KEY(`clave`))"
            )
        }

        private fun sembrarCategorias(db: SupportSQLiteDatabase) {
            CategoriasIniciales.lista.forEach { c ->
                db.execSQL(
                    "INSERT OR IGNORE INTO `categorias` " +
                        "(`clave`,`nombre`,`emoji`,`color`,`orden`,`subcategorias`,`palabrasClave`) " +
                        "VALUES (?,?,?,?,?,?,?)",
                    arrayOf<Any>(c.clave, c.nombre, c.emoji, c.color, c.orden, c.subcategorias, c.palabrasClave)
                )
            }
        }

        private val AL_CREAR = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                sembrarCategorias(db)
            }
        }

        fun obtener(context: Context): BaseDatos =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext, BaseDatos::class.java, "gastos.db"
                )
                    .addMigrations(MIGRACION_1_2, MIGRACION_2_3)
                    .addCallback(AL_CREAR)
                    .build().also { instancia = it }
            }
    }
}
