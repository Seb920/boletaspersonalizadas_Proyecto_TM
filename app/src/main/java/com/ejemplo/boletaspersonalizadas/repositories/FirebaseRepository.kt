package com.ejemplo.boletaspersonalizadas.repositories

import com.ejemplo.boletaspersonalizadas.models.*
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage

class FirebaseRepository {

    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance()

    // USUARIOS
    fun guardarUsuario(usuario: Usuario, callback: () -> Unit) {
        database.child("usuarios").child(usuario.id).setValue(usuario)
            .addOnSuccessListener { callback() }
            .addOnFailureListener { it.printStackTrace() }
    }

    fun obtenerUsuario(userId: String, callback: (Usuario?) -> Unit) {
        database.child("usuarios").child(userId).get()
            .addOnSuccessListener { snapshot ->
                val usuario = snapshot.getValue(Usuario::class.java)
                callback(usuario)
            }
            .addOnFailureListener {
                it.printStackTrace()
                callback(null)
            }
    }

    // PRODUCTOS
    fun agregarProducto(producto: Producto, callback: () -> Unit) {
        val key = database.child("productos").push().key ?: return
        val nuevoProducto = producto.copy(id = key)
        database.child("productos").child(key).setValue(nuevoProducto)
            .addOnSuccessListener { callback() }
            .addOnFailureListener { it.printStackTrace() }
    }

    fun actualizarProducto(producto: Producto, callback: () -> Unit) {
        database.child("productos").child(producto.id).setValue(producto)
            .addOnSuccessListener { callback() }
            .addOnFailureListener { it.printStackTrace() }
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
                it.printStackTrace()
                callback(emptyList())
            }
    }

    // CONFIGURACIÓN
    fun guardarConfiguracion(nombre: String, linkQr: String, igv: Double, callback: () -> Unit) {
        val config = ConfiguracionNegocio(
            nombreNegocio = nombre,
            linkQrNegocio = linkQr,
            porcentajeIGV = igv
        )
        database.child("configuracion").child("negocio").setValue(config)
            .addOnSuccessListener { callback() }
            .addOnFailureListener { it.printStackTrace() }
    }

    fun actualizarLogoUrl(url: String, callback: () -> Unit) {
        database.child("configuracion").child("negocio").child("logoUrl").setValue(url)
            .addOnSuccessListener { callback() }
            .addOnFailureListener { it.printStackTrace() }
    }

    fun obtenerConfiguracion(callback: (ConfiguracionNegocio?) -> Unit) {
        database.child("configuracion").child("negocio").get()
            .addOnSuccessListener { snapshot ->
                val config = snapshot.getValue(ConfiguracionNegocio::class.java)
                callback(config)
            }
            .addOnFailureListener {
                it.printStackTrace()
                callback(null)
            }
    }

    // BOLETAS
    fun guardarBoleta(boleta: Boleta, callback: (String) -> Unit) {
        val key = database.child("boletas").push().key ?: return
        val nuevaBoleta = boleta.copy(id = key)
        database.child("boletas").child(key).setValue(nuevaBoleta)
            .addOnSuccessListener { callback(key) }
            .addOnFailureListener {
                it.printStackTrace()
                callback("")
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
                it.printStackTrace()
                callback(emptyList())
            }
    }
}