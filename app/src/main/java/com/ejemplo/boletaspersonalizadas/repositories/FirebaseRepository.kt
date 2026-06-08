package com.ejemplo.boletaspersonalizadas.repositories

import android.util.Log
import com.ejemplo.boletaspersonalizadas.models.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class FirebaseRepository {

    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance()

    // ==================== USUARIOS ====================
    fun guardarUsuario(usuario: Usuario, callback: () -> Unit) {
        database.child("usuarios").child(usuario.id).setValue(usuario)
            .addOnSuccessListener { callback() }
            .addOnFailureListener { Log.e("Firebase", "Error guardar usuario", it) }
    }

    fun obtenerUsuario(userId: String, callback: (Usuario?) -> Unit) {
        database.child("usuarios").child(userId).get()
            .addOnSuccessListener { snapshot ->
                val usuario = snapshot.getValue(Usuario::class.java)
                callback(usuario)
            }
            .addOnFailureListener {
                Log.e("Firebase", "Error obtener usuario", it)
                callback(null)
            }
    }

    // ==================== PRODUCTOS ====================
    fun agregarProducto(producto: Producto, callback: () -> Unit) {
        val key = database.child("productos").push().key ?: return
        val nuevoProducto = producto.copy(id = key)
        database.child("productos").child(key).setValue(nuevoProducto)
            .addOnSuccessListener { callback() }
            .addOnFailureListener { Log.e("Firebase", "Error agregar producto", it) }
    }

    fun actualizarProducto(producto: Producto, callback: () -> Unit) {
        database.child("productos").child(producto.id).setValue(producto)
            .addOnSuccessListener { callback() }
            .addOnFailureListener { Log.e("Firebase", "Error actualizar producto", it) }
    }

    fun obtenerProductos(callback: (List<Producto>) -> Unit) {
        database.child("productos").get()
            .addOnSuccessListener { snapshot ->
                val productos = mutableListOf<Producto>()
                for (child in snapshot.children) {
                    val producto = child.getValue(Producto::class.java)
                    if (producto != null && producto.activo) {
                        productos.add(producto)
                    }
                }
                callback(productos)
            }
            .addOnFailureListener {
                Log.e("Firebase", "Error obtener productos", it)
                callback(emptyList())
            }
    }

    // ==================== CONFIGURACIÓN ====================
    fun guardarConfiguracionCompleta(
        nombre: String,
        ruc: String,
        direccion: String,
        linkQr: String,
        igv: Double,
        callback: () -> Unit
    ) {
        val config = ConfiguracionNegocio(
            nombreNegocio = nombre,
            ruc = ruc,
            direccion = direccion,
            linkQrNegocio = linkQr,
            porcentajeIGV = igv
        )
        database.child("configuracion").child("negocio").setValue(config)
            .addOnSuccessListener { callback() }
            .addOnFailureListener { Log.e("Firebase", "Error guardar configuración", it) }
    }

    fun actualizarLogoUrl(url: String, callback: () -> Unit) {
        database.child("configuracion").child("negocio").child("logoUrl").setValue(url)
            .addOnSuccessListener { callback() }
            .addOnFailureListener { Log.e("Firebase", "Error actualizar logo", it) }
    }

    fun obtenerConfiguracion(callback: (ConfiguracionNegocio?) -> Unit) {
        database.child("configuracion").child("negocio").get()
            .addOnSuccessListener { snapshot ->
                val config = snapshot.getValue(ConfiguracionNegocio::class.java)
                callback(config)
            }
            .addOnFailureListener {
                Log.e("Firebase", "Error obtener configuración", it)
                callback(null)
            }
    }

    // ==================== BOLETAS ====================
    fun guardarBoleta(boleta: Boleta, callback: (String) -> Unit) {
        val key = database.child("boletas").push().key ?: return
        val nuevaBoleta = boleta.copy(id = key)
        database.child("boletas").child(key).setValue(nuevaBoleta)
            .addOnSuccessListener {
                callback(key)
                // Guardar notificación automática
                guardarNotificacionNuevaBoleta(nuevaBoleta)
            }
            .addOnFailureListener {
                Log.e("Firebase", "Error guardar boleta", it)
                callback("")
            }
    }

    private fun guardarNotificacionNuevaBoleta(boleta: Boleta) {
        val notificacion = mapOf(
            "titulo" to "Nueva Boleta",
            "mensaje" to "Boleta ${boleta.numeroSerie} - Total: S/ ${String.format("%.2f", boleta.total)}",
            "fecha" to System.currentTimeMillis(),
            "boletaId" to boleta.id,
            "cliente" to boleta.clienteNombre
        )
        database.child("notificaciones").push().setValue(notificacion)
            .addOnFailureListener { Log.e("Firebase", "Error guardar notificación", it) }
    }

    fun obtenerNotificaciones(callback: (List<Map<String, Any>>) -> Unit) {
        database.child("notificaciones")
            .orderByChild("fecha")
            .limitToLast(20)
            .get()
            .addOnSuccessListener { snapshot ->
                val notificaciones = mutableListOf<Map<String, Any>>()
                for (child in snapshot.children) {
                    val noti = child.value as? Map<String, Any>
                    if (noti != null) {
                        notificaciones.add(noti)
                    }
                }
                callback(notificaciones.reversed())
            }
            .addOnFailureListener {
                Log.e("Firebase", "Error obtener notificaciones", it)
                callback(emptyList())
            }
    }

    fun obtenerBoletas(callback: (List<Boleta>) -> Unit) {
        database.child("boletas").get()
            .addOnSuccessListener { snapshot ->
                val boletas = mutableListOf<Boleta>()
                for (child in snapshot.children) {
                    val boleta = child.getValue(Boleta::class.java)
                    if (boleta != null) {
                        boletas.add(boleta)
                    }
                }
                callback(boletas.sortedByDescending { it.fecha })
            }
            .addOnFailureListener {
                Log.e("Firebase", "Error obtener boletas", it)
                callback(emptyList())
            }
    }
}