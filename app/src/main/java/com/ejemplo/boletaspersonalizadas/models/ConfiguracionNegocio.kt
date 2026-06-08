package com.ejemplo.boletaspersonalizadas.models

data class ConfiguracionNegocio(
    val id: String = "config_unica",
    val nombreNegocio: String = "Mi Negocio",
    val ruc: String = "20508565934",
    val direccion: String = "",
    val logoUrl: String = "",
    val porcentajeIGV: Double = 18.0,
    val linkQrNegocio: String = ""
)