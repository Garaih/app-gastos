package com.garaih.appgastos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garaih.appgastos.datos.Categoria
import com.garaih.appgastos.datos.Gasto
import com.garaih.appgastos.datos.GastosRepositorio
import com.garaih.appgastos.datos.Periodo
import com.garaih.appgastos.datos.Preferencias
import com.garaih.appgastos.datos.Recurrente
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.garaih.appgastos.captura.ServicioNotificaciones
import java.time.LocalDate
import kotlin.math.roundToInt

private sealed interface Pantalla {
    data object Inicio : Pantalla
    data object Analisis : Pantalla
    data object Ahorro : Pantalla
    data class Editar(val gasto: Gasto?) : Pantalla
    data object Buscar : Pantalla
    data object Categorias : Pantalla
    data object Recurrentes : Pantalla
    data class EditarCategoria(val categoria: Categoria?) : Pantalla
    data object Permisos : Pantalla
    data object Diagnostico : Pantalla
}

private val PESTANAS = listOf(Pantalla.Inicio, Pantalla.Analisis, Pantalla.Ahorro)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppGastos(permisoConcedido: Boolean, carteraInstalada: Boolean) {
    val context = LocalContext.current
    val repositorio = remember { GastosRepositorio.de(context) }
    val preferencias = remember { Preferencias(context) }
    val gastos by repositorio.gastos.collectAsState(initial = emptyList())
    val categorias by repositorio.categorias.collectAsState(initial = emptyList())
    val presupuestos by repositorio.presupuestos.collectAsState(initial = emptyList())
    val objetivos by repositorio.objetivos.collectAsState(initial = emptyList())
    val inversiones by repositorio.inversiones.collectAsState(initial = emptyList())
    val ingresos by repositorio.ingresos.collectAsState(initial = emptyList())
    val ingresosFijos by repositorio.ingresosFijos.collectAsState(initial = emptyList())
    val recurrentes by repositorio.recurrentes.collectAsState(initial = emptyList())
    // Al abrir la app se apuntan los cargos recurrentes que tocan hasta hoy.
    LaunchedEffect(Unit) {
        repositorio.generarRecurrentes()
        com.garaih.appgastos.recordatorios.Recordatorios.programar(context)
        com.garaih.appgastos.recordatorios.Recordatorios.revisar(context)
    }
    var saldoInicial by remember { mutableStateOf(preferencias.saldoInicial) }
    var inicioSaldo by remember { mutableStateOf(preferencias.inicioSaldo) }
    var dialogoSaldo by remember { mutableStateOf(false) }
    val mapaPresupuestos = presupuestos.associate { it.categoria to it.limiteCentimos }
    val scope = rememberCoroutineScope()

    var pantalla by remember { mutableStateOf<Pantalla>(Pantalla.Inicio) }
    var pestana by remember { mutableStateOf<Pantalla>(Pantalla.Inicio) }
    var diaInicio by remember { mutableStateOf(preferencias.diaInicioPeriodo) }
    var periodo by remember { mutableStateOf(Periodo.que(LocalDate.now(), diaInicio)) }
    var menuAbierto by remember { mutableStateOf(false) }
    var confirmarBorrado by remember { mutableStateOf(false) }
    var dialogoPeriodo by remember { mutableStateOf(false) }
    var dialogoActualizar by remember { mutableStateOf(false) }
    var recurrenteEditando by remember { mutableStateOf<Recurrente?>(null) }
    /** Resultado de "Revisar notificaciones ahora": (texto, lector parado). */
    var revision by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    /** Alta manual pendiente de confirmar porque se parece a un gasto ya guardado. */
    var duplicado by remember { mutableStateOf<Triple<Gasto, Gasto, () -> Unit>?>(null) }

    val enPestana = pantalla in PESTANAS

    fun volver() {
        confirmarBorrado = false
        pantalla = when (pantalla) {
            is Pantalla.EditarCategoria -> Pantalla.Categorias
            Pantalla.Analisis, Pantalla.Ahorro -> Pantalla.Inicio
            else -> pestana
        }
        if (pantalla in PESTANAS) pestana = pantalla
    }
    BackHandler(enabled = pantalla != Pantalla.Inicio) { volver() }

    val titulo = when (val p = pantalla) {
        Pantalla.Inicio -> "Resumen"
        Pantalla.Analisis -> "Análisis"
        Pantalla.Ahorro -> "Ahorro e inversiones"
        is Pantalla.Editar -> if (p.gasto == null) "Nuevo gasto" else "Editar gasto"
        Pantalla.Buscar -> "Buscar"
        Pantalla.Categorias -> "Categorías"
        Pantalla.Recurrentes -> "Fijos y suscripciones"
        is Pantalla.EditarCategoria -> if (p.categoria == null) "Nueva categoría" else "Editar categoría"
        Pantalla.Permisos -> "Permisos"
        Pantalla.Diagnostico -> "Diagnóstico de captura"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(titulo, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    if (!enPestana) {
                        IconButton(onClick = { volver() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                actions = {
                    val p = pantalla
                    if ((p is Pantalla.Editar && p.gasto != null) || (p is Pantalla.EditarCategoria && p.categoria != null)) {
                        IconButton(onClick = { confirmarBorrado = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Borrar")
                        }
                    }
                    if (p == Pantalla.Inicio) {
                        IconButton(onClick = { pantalla = Pantalla.Buscar }) {
                            Icon(Icons.Filled.Search, contentDescription = "Buscar")
                        }
                    }
                    if (enPestana) {
                        IconButton(onClick = { menuAbierto = true }) {
                            BadgedBox(badge = { if (!permisoConcedido) Badge() }) {
                                Icon(Icons.Filled.Settings, contentDescription = "Configuración")
                            }
                        }
                        DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                            DropdownMenuItem(
                                text = { Text("Categorías y presupuestos") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                                onClick = { menuAbierto = false; pantalla = Pantalla.Categorias },
                            )
                            DropdownMenuItem(
                                text = { Text("Fijos y suscripciones") },
                                leadingIcon = { Text("🔁") },
                                onClick = { menuAbierto = false; pantalla = Pantalla.Recurrentes },
                            )
                            DropdownMenuItem(
                                text = { Text("Inicio del periodo: día $diaInicio") },
                                leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                                onClick = { menuAbierto = false; dialogoPeriodo = true },
                            )
                            DropdownMenuItem(
                                text = { Text("Saldo acumulado") },
                                leadingIcon = { Text("💰") },
                                onClick = { menuAbierto = false; dialogoSaldo = true },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Revisar notificaciones ahora") },
                                leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                                onClick = {
                                    menuAbierto = false
                                    revision = "Revisando los avisos de la bandeja…" to false
                                    scope.launch { revision = revisarNotificaciones(context, permisoConcedido) }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Permisos de notificaciones") },
                                leadingIcon = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                                trailingIcon = { Text(if (permisoConcedido) "✅" else "⚠️") },
                                onClick = { menuAbierto = false; pantalla = Pantalla.Permisos },
                            )
                            DropdownMenuItem(
                                text = { Text("Diagnóstico de captura") },
                                leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                                onClick = { menuAbierto = false; pantalla = Pantalla.Diagnostico },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Buscar actualizaciones") },
                                leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                                trailingIcon = {
                                    Text(
                                        com.garaih.appgastos.BuildConfig.VERSION_NAME,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                                onClick = { menuAbierto = false; dialogoActualizar = true },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (enPestana) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                    listOf(
                        Triple(Pantalla.Inicio, "🏠", "Resumen"),
                        Triple(Pantalla.Analisis, "📊", "Análisis"),
                        Triple(Pantalla.Ahorro, "🐷", "Ahorro"),
                    ).forEach { (destino, icono, nombre) ->
                        NavigationBarItem(
                            selected = pantalla == destino,
                            onClick = { pantalla = destino; pestana = destino },
                            icon = { Text(icono, fontSize = 20.sp) },
                            label = { Text(nombre) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (pantalla == Pantalla.Inicio) {
                ExtendedFloatingActionButton(
                    onClick = { pantalla = Pantalla.Editar(null) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Añadir") },
                )
            }
        },
    ) { padding ->
        val mod = Modifier.padding(padding)
        val inicioPeriodo = if (inicioSaldo > 0) Periodo(java.time.YearMonth.of(inicioSaldo / 100, inicioSaldo % 100), diaInicio)
            else com.garaih.appgastos.datos.Saldo.inicioAutomatico(diaInicio, ingresos, ingresosFijos, ::aFechaLocal)
        val saldo = com.garaih.appgastos.datos.Saldo.calcular(periodo, inicioPeriodo, saldoInicial, gastos, ingresos, ingresosFijos, ::aFechaLocal)
        when (val p = pantalla) {
            Pantalla.Inicio -> PantallaInicio(
                gastos = gastos,
                categorias = categorias,
                presupuestos = mapaPresupuestos,
                ingresos = ingresos,
                ingresosFijos = ingresosFijos,
                recurrentes = recurrentes,
                arrastre = saldo.arrastre,
                apartado = objetivos.sumOf { it.ahorradoCentimos },
                periodo = periodo,
                onCambiarPeriodo = { periodo = it },
                onAbrirGasto = { pantalla = Pantalla.Editar(it) },
                onIrAhorro = { pantalla = Pantalla.Ahorro; pestana = Pantalla.Ahorro },
                onIrFijos = { pantalla = Pantalla.Recurrentes },
                modifier = mod,
            )
            Pantalla.Analisis -> PantallaAnalisis(
                gastos = gastos,
                categorias = categorias,
                periodo = periodo,
                onCambiarPeriodo = { periodo = it },
                modifier = mod,
            )
            Pantalla.Ahorro -> PantallaAhorro(
                gastos = gastos,
                ingresos = ingresos,
                ingresosFijos = ingresosFijos,
                objetivos = objetivos,
                inversiones = inversiones,
                periodo = periodo,
                onCambiarPeriodo = { periodo = it },
                acciones = AccionesAhorro(
                    cambiarIngresoFijo = { anterior, concepto, importe, p -> scope.launch { repositorio.cambiarIngresoFijo(anterior, concepto, importe, p) } },
                    guardarIngreso = { scope.launch { repositorio.guardarIngreso(it) } },
                    borrarIngreso = { scope.launch { repositorio.borrarIngreso(it) } },
                    guardarObjetivo = { scope.launch { repositorio.guardarObjetivo(it) } },
                    borrarObjetivo = { scope.launch { repositorio.borrarObjetivo(it) } },
                    guardarInversion = { scope.launch { repositorio.guardarInversion(it) } },
                    borrarInversion = { scope.launch { repositorio.borrarInversion(it) } },
                ),
                modifier = mod,
            )
            is Pantalla.Editar -> PantallaEditarGasto(
                gasto = p.gasto,
                categorias = categorias,
                recurrente = p.gasto?.recurrenteId?.let { id -> recurrentes.firstOrNull { it.id == id } },
                onGuardar = { g, recordar, repeticion ->
                    val guardarYa = {
                        scope.launch {
                            if (repeticion != null) repositorio.guardarComoRecurrente(g, repeticion.plazos, recordar)
                            else repositorio.guardar(g, recordar)
                        }
                        periodo = Periodo.que(aFechaLocal(g.fecha), diaInicio)
                        volver()
                    }
                    if (g.id != 0L) guardarYa()
                    else scope.launch {
                        val parecido = repositorio.posibleDuplicado(g)
                        if (parecido == null) guardarYa() else duplicado = Triple(g, parecido, guardarYa)
                    }
                },
                onGestionarRecurrente = { recurrenteEditando = it },
                onBorrar = { g ->
                    scope.launch { repositorio.borrar(g) }
                    volver()
                },
                mostrarConfirmarBorrado = confirmarBorrado,
                onCerrarConfirmarBorrado = { confirmarBorrado = false },
                modifier = mod,
            )
            Pantalla.Buscar -> PantallaBuscar(
                gastos = gastos,
                categorias = categorias,
                onAbrirGasto = { pantalla = Pantalla.Editar(it) },
                modifier = mod,
            )
            Pantalla.Recurrentes -> PantallaRecurrentes(
                recurrentes = recurrentes,
                gastos = gastos,
                categorias = categorias,
                onEditar = { recurrenteEditando = it },
                onAceptarSugerencia = { scope.launch { repositorio.aceptarSugerencia(it) } },
                modifier = mod,
            )
            Pantalla.Categorias -> PantallaCategorias(
                categorias = categorias,
                onEditar = { pantalla = Pantalla.EditarCategoria(it) },
                modifier = mod,
            )
            is Pantalla.EditarCategoria -> PantallaEditarCategoria(
                categoria = p.categoria,
                presupuestoInicial = p.categoria?.let { mapaPresupuestos[it.clave] },
                onGuardar = { c, limite ->
                    scope.launch {
                        repositorio.guardarCategoria(c)
                        repositorio.guardarPresupuesto(c.clave, limite)
                    }
                    volver()
                },
                onBorrar = { c ->
                    scope.launch { repositorio.borrarCategoria(c) }
                    volver()
                },
                mostrarConfirmarBorrado = confirmarBorrado,
                onCerrarConfirmarBorrado = { confirmarBorrado = false },
                modifier = mod,
            )
            Pantalla.Permisos -> PantallaPermisos(permisoConcedido, carteraInstalada, mod)
            Pantalla.Diagnostico -> PantallaDiagnostico(mod)
        }
    }

    recurrenteEditando?.let { r ->
        DialogoRecurrente(
            r = r,
            onGuardar = { scope.launch { repositorio.guardarRecurrente(it) } },
            onFinalizar = { scope.launch { repositorio.finalizarRecurrente(it) } },
            onBorrar = { scope.launch { repositorio.borrarRecurrente(it) } },
            onCerrar = { recurrenteEditando = null },
        )
    }

    duplicado?.let { (nuevo, existente, guardarYa) ->
        AlertDialog(
            onDismissRequest = { duplicado = null },
            title = { Text("¿Gasto repetido?") },
            text = {
                Text(
                    "Ya tienes ese día un gasto de ${euros(existente.importeCentimos ?: 0)} en " +
                        "\"${existente.comercio}\" (${fechaHora(existente.fecha)}). ¿Añadir \"${nuevo.comercio}\" igualmente?"
                )
            },
            confirmButton = { TextButton(onClick = { duplicado = null; guardarYa() }) { Text("Añadir igualmente") } },
            dismissButton = { TextButton(onClick = { duplicado = null }) { Text("No, es el mismo") } },
        )
    }

    if (dialogoSaldo) {
        var texto by remember { mutableStateOf(importeEditable(saldoInicial.takeIf { it != 0L })) }
        val desde = if (inicioSaldo > 0) Periodo(java.time.YearMonth.of(inicioSaldo / 100, inicioSaldo % 100), diaInicio)
            else com.garaih.appgastos.datos.Saldo.inicioAutomatico(diaInicio, ingresos, ingresosFijos, ::aFechaLocal)
        AlertDialog(
            onDismissRequest = { dialogoSaldo = false },
            title = { Text("Saldo acumulado") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Lo que sobra de un periodo pasa al siguiente. El Resumen muestra lo disponible en total: " +
                            "saldo inicial + ingresos − gastos desde el periodo de inicio, menos lo apartado en objetivos.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Se cuenta desde: " + (desde?.let { nombrePeriodo(it) } ?: "tu primer ingreso") +
                            if (inicioSaldo > 0) "" else " (automático)",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    CampoImporte(texto, { texto = it }, "Saldo inicial (opcional)")
                    TextButton(onClick = {
                        val actual = Periodo.que(LocalDate.now(), diaInicio)
                        inicioSaldo = actual.mes.year * 100 + actual.mes.monthValue
                        preferencias.inicioSaldo = inicioSaldo
                    }) { Text("Empezar a contar desde este periodo") }
                    if (inicioSaldo > 0) {
                        TextButton(onClick = { inicioSaldo = 0; preferencias.inicioSaldo = 0 }) { Text("Volver al inicio automático") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    saldoInicial = centimosDe(texto) ?: 0L
                    preferencias.saldoInicial = saldoInicial
                    dialogoSaldo = false
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { dialogoSaldo = false }) { Text("Cancelar") } },
        )
    }

    revision?.let { (texto, parado) ->
        AlertDialog(
            onDismissRequest = { revision = null },
            title = { Text("Revisar notificaciones") },
            text = { Text(texto) },
            confirmButton = { TextButton(onClick = { revision = null }) { Text("Aceptar") } },
            dismissButton = if (parado) {
                {
                    TextButton(onClick = {
                        revision = null
                        context.startActivity(
                            android.content.Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                android.net.Uri.parse("package:${context.packageName}"),
                            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }) { Text("Ajustes de la app") }
                }
            } else null,
        )
    }

    if (dialogoActualizar) DialogoActualizar(onCerrar = { dialogoActualizar = false })

    if (dialogoPeriodo) {
        var valor by remember { mutableFloatStateOf(diaInicio.toFloat()) }
        val dia = valor.roundToInt()
        AlertDialog(
            onDismissRequest = { dialogoPeriodo = false },
            title = { Text("Inicio del periodo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (dia == 1) "Día 1: meses naturales."
                        else "Día $dia: cada periodo va del $dia de un mes al ${dia - 1} del siguiente.",
                    )
                    Text("Día $dia", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Slider(value = valor, onValueChange = { valor = it }, valueRange = 1f..28f, steps = 26)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    preferencias.diaInicioPeriodo = dia
                    diaInicio = dia
                    periodo = Periodo.que(LocalDate.now(), dia)
                    dialogoPeriodo = false
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { dialogoPeriodo = false }) { Text("Cancelar") } },
        )
    }
}

/**
 * Procesa los avisos que hay ahora en la bandeja. Si el sistema ha parado el lector (MIUI),
 * pide a Android que lo vuelva a conectar. Devuelve (mensaje, lector parado).
 */
private suspend fun revisarNotificaciones(context: android.content.Context, permiso: Boolean): Pair<String, Boolean> {
    if (!permiso) return "Primero concede el acceso a notificaciones en ⚙️ → Permisos de notificaciones." to false
    var servicio = ServicioNotificaciones.instancia
    if (servicio == null) {
        android.service.notification.NotificationListenerService.requestRebind(
            android.content.ComponentName(context, ServicioNotificaciones::class.java)
        )
        repeat(10) { if (ServicioNotificaciones.instancia == null) delay(500) }
        servicio = ServicioNotificaciones.instancia
    }
    if (servicio == null) {
        return ("El sistema tiene parado el lector de notificaciones, por eso no se apuntan los pagos. " +
            "En los ajustes de la app activa \"Inicio automático\" y en Ahorro de batería elige \"Sin restricciones\". " +
            "Si sigue igual, desactiva y vuelve a activar el acceso a notificaciones de Gastos.") to true
    }
    val (revisados, nuevos) = servicio.revisarBandeja()
    return when {
        revisados == 0 -> "No hay avisos de pagos en la bandeja ahora mismo. Los que ya se han borrado de la bandeja no se pueden recuperar; añádelos a mano."
        nuevos == 0 -> "Revisados $revisados avisos: ya estaban todos apuntados (los avisos repetidos del mismo pago se unen)."
        else -> "Revisados $revisados avisos: $nuevos ${if (nuevos == 1) "gasto nuevo apuntado" else "gastos nuevos apuntados"}."
    } to false
}
