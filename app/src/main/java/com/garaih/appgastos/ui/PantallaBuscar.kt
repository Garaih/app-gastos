package com.garaih.appgastos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.garaih.appgastos.datos.Categoria
import com.garaih.appgastos.datos.Clasificador
import com.garaih.appgastos.datos.Gasto

/** Busca en todos los gastos por comercio, categoría o subcategoría. */
@Composable
fun PantallaBuscar(
    gastos: List<Gasto>,
    categorias: List<Categoria>,
    onAbrirGasto: (Gasto) -> Unit,
    modifier: Modifier = Modifier,
) {
    var texto by rememberSaveable { mutableStateOf("") }
    val nombres = remember(categorias) { categorias.associate { it.clave to Clasificador.normalizar(it.nombre) } }
    val q = Clasificador.normalizar(texto)
    val resultados = remember(gastos, q) {
        if (q.length < 2) emptyList() else gastos.filter { g ->
            Clasificador.normalizar(g.comercio).contains(q) ||
                (g.categoria?.let { nombres[it] }?.contains(q) == true) ||
                (g.subcategoria?.let { Clasificador.normalizar(it).contains(q) } == true)
        }
    }
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text("Comercio, categoría o subcategoría") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (q.length >= 2) {
            Text(
                "${resultados.size} resultados · ${euros(resultados.sumOf { it.importeCentimos ?: 0L })}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn {
            items(resultados, key = { it.id }) { g -> FilaGasto(g, onClick = { onAbrirGasto(g) }) }
        }
    }
}
