package com.garaih.appgastos.datos

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Gasto::class, ReglaCategoria::class, Categoria::class,
        Presupuesto::class, Objetivo::class, Inversion::class, Ingreso::class, IngresoFijo::class, Recurrente::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class BaseDatos : RoomDatabase() {
    abstract fun gastoDao(): GastoDao
    abstract fun reglaDao(): ReglaCategoriaDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun finanzasDao(): FinanzasDao
    abstract fun recurrenteDao(): RecurrenteDao

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

        private val MIGRACION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `presupuestos` (`categoria` TEXT NOT NULL, " +
                        "`limiteCentimos` INTEGER NOT NULL, PRIMARY KEY(`categoria`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `objetivos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`nombre` TEXT NOT NULL, `emoji` TEXT NOT NULL, `objetivoCentimos` INTEGER NOT NULL, " +
                        "`ahorradoCentimos` INTEGER NOT NULL, `fechaLimite` INTEGER, `creado` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `inversiones` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`nombre` TEXT NOT NULL, `tipo` TEXT NOT NULL, `invertidoCentimos` INTEGER NOT NULL, " +
                        "`valorCentimos` INTEGER NOT NULL, `actualizado` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ingresos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`fecha` INTEGER NOT NULL, `importeCentimos` INTEGER NOT NULL, `concepto` TEXT NOT NULL)"
                )
            }
        }

        private val MIGRACION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ingresos_fijos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`concepto` TEXT NOT NULL, `importeCentimos` INTEGER NOT NULL, `desdeMes` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRACION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `gastos` ADD COLUMN `recurrenteId` INTEGER")
                db.execSQL("ALTER TABLE `gastos` ADD COLUMN `plazo` INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recurrentes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`comercio` TEXT NOT NULL, `importeCentimos` INTEGER NOT NULL, `categoria` TEXT, " +
                        "`subcategoria` TEXT, `dia` INTEGER NOT NULL, `inicioMes` INTEGER NOT NULL, " +
                        "`plazos` INTEGER, `ultimoMes` INTEGER NOT NULL, `activo` INTEGER NOT NULL, " +
                        "`creado` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRACION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `recurrentes` ADD COLUMN `recordarDias` INTEGER")
                db.execSQL("ALTER TABLE `recurrentes` ADD COLUMN `quiereCancelar` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `recurrentes` ADD COLUMN `avisadoMes` INTEGER")
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
                    .addMigrations(MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4, MIGRACION_4_5, MIGRACION_5_6, MIGRACION_6_7)
                    .addCallback(AL_CREAR)
                    .build().also { instancia = it }
            }
    }
}
