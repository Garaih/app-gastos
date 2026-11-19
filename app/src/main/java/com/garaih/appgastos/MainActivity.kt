package com.garaih.appgastos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.garaih.appgastos.captura.Cartera
import com.garaih.appgastos.ui.AppGastos
import com.garaih.appgastos.ui.theme.AppGastosTheme

class MainActivity : ComponentActivity() {

    private var permisoConcedido by mutableStateOf(false)
    private var carteraInstalada by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppGastosTheme {
                AppGastos(permisoConcedido = permisoConcedido, carteraInstalada = carteraInstalada)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Se recalcula al volver de Ajustes.
        permisoConcedido = Cartera.permisoConcedido(this)
        carteraInstalada = Cartera.estaInstalada(this)
    }
}
