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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.garaih.appgastos.datos.Categoria
import com.garaih.appgastos.datos.Gasto
import com.garaih.appgastos.datos.GastosRepositorio
import com.garaih.appgastos.datos.Periodo
import com.garaih.appgastos.datos.Preferencias
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

private sealed interface Pantalla {
    data object Inicio : Pantalla
    data class Editar(val gasto: Gasto?) : Pantalla
    data object Categorias : Pantalla
    data class EditarCategoria(val categoria: Categoria?) : Pantalla
    data object Permisos : Pantalla
    data object Diagnostico : Pantalla
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppGastos(permisoConcedido: Boolean, carteraInstalada: Boolean) {
    val context = LocalContext.current
    val repositorio = remember { GastosRepositorio.de(context) }
    val preferencias = remember { Preferencias(context) }
    val gastos by repositorio.gastos.collectAsState(initial = emptyList())
    val categorias by repositorio.categorias.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var pantalla by remember { mutableStateOf<Pantalla>(Pantalla.Inicio) }
    var diaInicio by remember { mutableStateOf(preferencias.diaInicioPeriodo) }
    var periodo by remember { mutableStateOf(Periodo.que(LocalDate.now(), diaInicio)) }
    var menuAbierto by remember { mutableStateOf(false) }
    var confirmarBorrado by remember { mutableStateOf(false) }
    var dialogoPeriodo by remember { mutableStateOf(false) }

    fun volver() {
        confirmarBorrado = false
        pantalla = when (pantalla) {
            is Pantalla.EditarCategoria -> Pantalla.Categorias
            else -> Pantalla.Inicio
        }
    }
    BackHandler(enabled = pantalla != Pantalla.Inicio) { volver() }

    val titulo = when (val p = pantalla) {
        Pantalla.Inicio -> "Mis gastos"
        is Pantalla.Editar -> if (p.gasto == null) "Nuevo gasto" else "Editar gasto"
        Pantalla.Categorias -> "Categorías"
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
                    if (pantalla != Pantalla.Inicio) {
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
                        IconButton(onClick = { menuAbierto = true }) {
                            BadgedBox(badge = { if (!permisoConcedido) Badge() }) {
                                Icon(Icons.Filled.Settings, contentDescription = "Configuración")
                            }
                        }
                        DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                            DropdownMenuItem(
                                text = { Text("Categorías") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                                onClick = { menuAbierto = false; pantalla = Pantalla.Categorias },
                            )
                            DropdownMenuItem(
                                text = { Text("Inicio del periodo: día $diaInicio") },
                                leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                                onClick = { menuAbierto = false; dialogoPeriodo = true },
                            )
                            HorizontalDivider()
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
                        }
                    }
                },
            )
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
        when (val p = pantalla) {
            Pantalla.Inicio -> PantallaInicio(
                gastos = gastos,
                categorias = categorias,
                periodo = periodo,
                onCambiarPeriodo = { periodo = it },
                onAbrirGasto = { pantalla = Pantalla.Editar(it) },
                modifier = mod,
            )
            is Pantalla.Editar -> PantallaEditarGasto(
                gasto = p.gasto,
                categorias = categorias,
                onGuardar = { g, recordar ->
                    scope.launch { repositorio.guardar(g, recordar) }
                    periodo = Periodo.que(aFechaLocal(g.fecha), diaInicio)
                    pantalla = Pantalla.Inicio
                },
                onBorrar = { g ->
                    scope.launch { repositorio.borrar(g) }
                    volver()
                },
                mostrarConfirmarBorrado = confirmarBorrado,
                onCerrarConfirmarBorrado = { confirmarBorrado = false },
                modifier = mod,
            )
            Pantalla.Categorias -> PantallaCategorias(
                categorias = categorias,
                onEditar = { pantalla = Pantalla.EditarCategoria(it) },
                modifier = mod,
            )
            is Pantalla.EditarCategoria -> PantallaEditarCategoria(
                categoria = p.categoria,
                onGuardar = { c ->
                    scope.launch { repositorio.guardarCategoria(c) }
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

