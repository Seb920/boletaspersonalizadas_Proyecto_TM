package com.ejemplo.boletaspersonalizadas.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenciasManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun guardarSesion(userId: String, rol: String, nombre: String) {
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_id", userId)
            putString("rol", rol)
            putString("nombre", nombre)
            apply()
        }
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean("is_logged_in", false)

    fun getUserId(): String = prefs.getString("user_id", "") ?: ""

    fun getRol(): String = prefs.getString("rol", "") ?: ""

    fun getNombre(): String = prefs.getString("nombre", "") ?: ""

    fun limpiarSesion() {
        prefs.edit().clear().apply()
    }
}