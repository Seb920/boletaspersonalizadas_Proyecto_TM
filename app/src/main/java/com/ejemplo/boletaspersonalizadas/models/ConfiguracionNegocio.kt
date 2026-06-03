package com.ejemplo.boletaspersonalizadas.models

data class ConfiguracionNegocio(
    val id: String = "config_unica",
    val nombreNegocio: String = "Mi Negocio",
    val logoUrl: String = "",
    val porcentajeIGV: Double = 18.0,
    val linkQrNegocio: String = "https://mipagina.com"
)