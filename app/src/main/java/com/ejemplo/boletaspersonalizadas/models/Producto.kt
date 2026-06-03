package com.ejemplo.boletaspersonalizadas.models

data class Producto(
    val id: String = "",
    val codigo: String = "",
    val nombre: String = "",
    val precioUnitario: Double = 0.0,
    val activo: Boolean = true
)