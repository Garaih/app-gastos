package com.garaih.appgastos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.garaih.appgastos.ui.theme.AppGastosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppGastosTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    PantallaInicio(Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
fun PantallaInicio(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("App de gastos", style = MaterialTheme.typography.headlineMedium)
        Text("Esqueleto v0.1. Siguiente paso: captura de notificaciones de la cartera.")
    }
}

@Preview(showBackground = true)
@Composable
private fun PantallaInicioPreview() {
    AppGastosTheme { PantallaInicio() }
}
