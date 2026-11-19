package com.garaih.appgastos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garaih.appgastos.datos.Categoria
import com.garaih.appgastos.datos.Subcategoria

private val PALETA = listOf(
    0xFF3B82F6, 0xFF22A55E, 0xFFF59E0B, 0xFFEF4444, 0xFFA855F7,
    0xFF06B6D4, 0xFFEC4899, 0xFF64748B, 0xFF84CC16, 0xFFF97316, 0xFF0EA5E9, 0xFF8B5CF6,
)

/** Ajustes > Categorías: lista. */
@Composable
fun PantallaCategorias(
    categorias: List<Categoria>,
    onEditar: (Categoria?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "Toca una categoría para cambiar su nombre, color, subcategorías o las palabras con las que se reconoce automáticamente.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(categorias, key = { it.clave }) { c ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onEditar(c) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Color(c.color).copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) { Text(c.emoji, fontSize = 20.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        val subs = c.listaSubcategorias().joinToString(", ") { it.nombre }
                        Text(
                            subs.ifEmpty { "Sin subcategorías" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = { onEditar(null) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Nueva categoría")
            }
        }
    }
}

/** Alta o edición de una categoría. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantallaEditarCategoria(
    categoria: Categoria?,
    presupuestoInicial: Long?,
    onGuardar: (Categoria, Long?) -> Unit,
    onBorrar: (Categoria) -> Unit,
    mostrarConfirmarBorrado: Boolean,
    onCerrarConfirmarBorrado: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var nombre by remember { mutableStateOf(categoria?.nombre ?: "") }
    var emoji by remember { mutableStateOf(categoria?.emoji ?: "🏷️") }
    var color by remember { mutableStateOf(categoria?.color ?: PALETA.first()) }
    var presupuesto by remember { mutableStateOf(importeEditable(presupuestoInicial)) }
    var palabras by remember { mutableStateOf(categoria?.listaPalabras()?.joinToString(", ") ?: "") }
    val subs = remember {
        mutableStateListOf<Pair<String, String>>().apply {
            categoria?.listaSubcategorias()?.forEach { add(it.nombre to it.palabras.joinToString(", ")) }
        }
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = emoji,
                onValueChange = { emoji = it.take(4) },
                label = { Text("Icono") },
                singleLine = true,
                modifier = Modifier.width(90.dp),
            )
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        Text("Color", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PALETA.forEach { c ->
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(c))
                        .border(
                            width = if (c == color) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape,
                        )
                        .clickable { color = c }
                )
            }
        }

        CampoImporte(presupuesto, { presupuesto = it }, "Presupuesto por periodo (opcional)")

        OutlinedTextField(
            value = palabras,
            onValueChange = { palabras = it },
            label = { Text("Palabras clave (separadas por comas)") },
            supportingText = { Text("Si el comercio contiene alguna, el gasto entra aquí solo.") },
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Subcategorías", style = MaterialTheme.typography.titleSmall)
        subs.forEachIndexed { i, (n, p) ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = n,
                            onValueChange = { subs[i] = it to p },
                            label = { Text("Subcategoría") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { subs.removeAt(i) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Quitar subcategoría")
                        }
                    }
                    OutlinedTextField(
                        value = p,
                        onValueChange = { subs[i] = n to it },
                        label = { Text("Palabras clave") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        OutlinedButton(onClick = { subs.add("" to "") }) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("  Añadir subcategoría")
        }

        Button(
            onClick = {
                val lista = subs.map { (n, p) -> Subcategoria(n.trim(), Categoria.decodificarPalabras(p)) }
                onGuardar(
                    Categoria(
                        clave = categoria?.clave ?: ("c" + System.currentTimeMillis()),
                        nombre = nombre.trim(),
                        emoji = emoji.trim().ifEmpty { "🏷️" },
                        color = color,
                        orden = categoria?.orden ?: 0,
                        subcategorias = Categoria.codificarSubcategorias(lista),
                        palabrasClave = Categoria.decodificarPalabras(palabras).joinToString(","),
                    ),
                    centimosDe(presupuesto),
                )
            },
            enabled = nombre.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (categoria == null) "Crear categoría" else "Guardar cambios") }
    }

    if (mostrarConfirmarBorrado && categoria != null) {
        AlertDialog(
            onDismissRequest = onCerrarConfirmarBorrado,
            title = { Text("¿Borrar \"${categoria.nombre}\"?") },
            text = { Text("Sus gastos no se borran: quedarán como \"Sin categoría\".") },
            confirmButton = { TextButton(onClick = { onBorrar(categoria) }) { Text("Borrar") } },
            dismissButton = { TextButton(onClick = onCerrarConfirmarBorrado) { Text("Cancelar") } },
        )
    }
}
