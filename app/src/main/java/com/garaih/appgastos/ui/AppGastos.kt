package com.garaih.appgastos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.garaih.appgastos.datos.Gasto
import com.garaih.appgastos.datos.GastosRepositorio
import kotlinx.coroutines.launch
import java.time.YearMonth

private sealed interface Pantalla {
    data object Inicio : Pantalla
    data class Editar(val gasto: Gasto?) : Pantalla
    data object Permisos : Pantalla
    data object Diagnostico : Pantalla
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppGastos(permisoConcedido: Boolean, carteraInstalada: Boolean) {
    val context = LocalContext.current
    val repositorio = remember { GastosRepositorio.de(context) }
    val gastos by repositorio.gastos.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var pantalla by remember { mutableStateOf<Pantalla>(Pantalla.Inicio) }
    var mes by remember { mutableStateOf(YearMonth.now()) }
    var menuAbierto by remember { mutableStateOf(false) }
    var confirmarBorrado by remember { mutableStateOf(false) }

    BackHandler(enabled = pantalla != Pantalla.Inicio) { pantalla = Pantalla.Inicio }

    val titulo = when (val p = pantalla) {
        Pantalla.Inicio -> "Mis gastos"
        is Pantalla.Editar -> if (p.gasto == null) "Nuevo gasto" else "Editar gasto"
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
                        IconButton(onClick = { pantalla = Pantalla.Inicio }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                actions = {
                    val p = pantalla
                    if (p is Pantalla.Editar && p.gasto != null) {
                        IconButton(onClick = { confirmarBorrado = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Borrar gasto")
                        }
                    }
                    if (pantalla == Pantalla.Inicio) {
                        IconButton(onClick = { menuAbierto = true }) {
                            BadgedBox(badge = { if (!permisoConcedido) Badge() }) {
                                Icon(Icons.Filled.Settings, contentDescription = "Configuración")
                            }
                        }
                        DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
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
                mes = mes,
                onCambiarMes = { mes = it },
                onAbrirGasto = { pantalla = Pantalla.Editar(it) },
                modifier = mod,
            )
            is Pantalla.Editar -> PantallaEditarGasto(
                gasto = p.gasto,
                onGuardar = { g, recordar ->
                    scope.launch { repositorio.guardar(g, recordar) }
                    mes = YearMonth.from(aFechaLocal(g.fecha))
                    pantalla = Pantalla.Inicio
                },
                onBorrar = { g ->
                    scope.launch { repositorio.borrar(g) }
                    confirmarBorrado = false
                    pantalla = Pantalla.Inicio
                },
                mostrarConfirmarBorrado = confirmarBorrado,
                onCerrarConfirmarBorrado = { confirmarBorrado = false },
                modifier = mod,
            )
            Pantalla.Permisos -> PantallaPermisos(permisoConcedido, carteraInstalada, mod)
            Pantalla.Diagnostico -> PantallaDiagnostico(mod)
        }
    }
}
