package com.garaih.appgastos.captura

/** Texto tal cual llega en una notificación de la cartera. Solo vive en memoria (spike). */
data class NotificacionCruda(
    val clave: String,
    val paquete: String,
    val momento: Long,
    val titulo: String?,
    val texto: String?,
    val textoGrande: String?,
    val subTexto: String?,
    val textoInfo: String?,
)
