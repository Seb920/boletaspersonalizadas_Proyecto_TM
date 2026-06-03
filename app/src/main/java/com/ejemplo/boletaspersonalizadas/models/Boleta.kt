package com.ejemplo.boletaspersonalizadas.models

data class Boleta(
    val id: String = "",
    val numeroSerie: String = "",
    val fecha: Long = System.currentTimeMillis(),
    val productos: List<ItemBoleta> = emptyList(),
    val subtotal: Double = 0.0,
    val igv: Double = 0.0,
    val total: Double = 0.0,
    val clienteNombre: String = "",
    val clienteDocumento: String = "",
    val empleadoId: String = "",
    val empleadoNombre: String = "",
    val linkQr: String = ""
)

data class ItemBoleta(
    val codigo: String = "",
    val nombre: String = "",
    val cantidad: Int = 1,
    val precioUnitario: Double = 0.0,
    val total: Double = 0.0
)