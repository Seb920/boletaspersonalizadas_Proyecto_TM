package com.ejemplo.boletaspersonalizadas.models

data class Usuario(
    val id: String = "",
    val email: String = "",
    val nombre: String = "",
    val rol: String = "", // "admin" o "empleado"
    val fechaRegistro: Long = System.currentTimeMillis()
)