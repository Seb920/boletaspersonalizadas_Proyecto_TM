package com.ejemplo.boletaspersonalizadas.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.ejemplo.boletaspersonalizadas.R
import com.ejemplo.boletaspersonalizadas.databinding.ActivityAdminBinding
import com.ejemplo.boletaspersonalizadas.models.Producto
import com.ejemplo.boletaspersonalizadas.repositories.FirebaseRepository
import com.ejemplo.boletaspersonalizadas.utils.PreferenciasManager
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private lateinit var firebaseRepo: FirebaseRepository
    private lateinit var preferencias: PreferenciasManager
    private lateinit var storage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseRepo = FirebaseRepository()
        preferencias = PreferenciasManager(this)
        storage = FirebaseStorage.getInstance()

        configurarTabs()
        cargarConfiguracion()
        cargarProductos()
        cargarNotificaciones()  // Llamar a la función de notificaciones

        binding.btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            preferencias.limpiarSesion()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun configurarTabs() {
        binding.btnTabProductos.setOnClickListener {
            binding.tabProductos.visibility = android.view.View.VISIBLE
            binding.tabConfiguracion.visibility = android.view.View.GONE
        }

        binding.btnTabConfiguracion.setOnClickListener {
            binding.tabProductos.visibility = android.view.View.GONE
            binding.tabConfiguracion.visibility = android.view.View.VISIBLE
        }

        binding.btnAgregarProducto.setOnClickListener {
            mostrarDialogoProducto(null)
        }

        binding.btnCambiarLogo.setOnClickListener {
            seleccionarLogo()
        }

        binding.btnGuardarConfiguracion.setOnClickListener {
            guardarConfiguracion()
        }
    }

    private fun mostrarDialogoProducto(producto: Producto?) {
        val view = layoutInflater.inflate(R.layout.dialog_producto, null)
        val etCodigo = view.findViewById<TextInputEditText>(R.id.etCodigo)
        val etNombre = view.findViewById<TextInputEditText>(R.id.etNombre)
        val etPrecio = view.findViewById<TextInputEditText>(R.id.etPrecio)

        if (producto != null) {
            etCodigo.setText(producto.codigo)
            etNombre.setText(producto.nombre)
            etPrecio.setText(producto.precioUnitario.toString())
        }

        AlertDialog.Builder(this)
            .setTitle(if (producto == null) "Agregar Producto" else "Editar Producto")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val codigo = etCodigo.text.toString()
                val nombre = etNombre.text.toString()
                val precio = etPrecio.text.toString().toDoubleOrNull() ?: 0.0

                if (codigo.isNotEmpty() && nombre.isNotEmpty() && precio > 0) {
                    val nuevoProducto = Producto(
                        id = producto?.id ?: "",
                        codigo = codigo,
                        nombre = nombre,
                        precioUnitario = precio,
                        activo = true
                    )

                    if (producto == null) {
                        firebaseRepo.agregarProducto(nuevoProducto) {
                            Toast.makeText(this, "Producto agregado", Toast.LENGTH_SHORT).show()
                            cargarProductos()
                        }
                    } else {
                        firebaseRepo.actualizarProducto(nuevoProducto) {
                            Toast.makeText(this, "Producto actualizado", Toast.LENGTH_SHORT).show()
                            cargarProductos()
                        }
                    }
                } else {
                    Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cargarProductos() {
        firebaseRepo.obtenerProductos { productos ->
            val texto = if (productos.isEmpty()) {
                "No hay productos. Presiona + para agregar"
            } else {
                productos.joinToString("\n") {
                    "${it.codigo} - ${it.nombre} - S/ ${String.format("%.2f", it.precioUnitario)}"
                }
            }
            binding.tvListaProductos.text = texto
        }
    }

    private fun seleccionarLogo() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK && data?.data != null) {
            val uri = data.data!!
            subirLogoAFirebase(uri)
        }
    }

    private fun subirLogoAFirebase(uri: Uri) {
        val referencia = storage.reference.child("logos/logo_negocio.png")
        referencia.putFile(uri).addOnSuccessListener {
            referencia.downloadUrl.addOnSuccessListener { url ->
                firebaseRepo.actualizarLogoUrl(url.toString()) {
                    cargarConfiguracion()
                    Toast.makeText(this, "Logo actualizado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun cargarConfiguracion() {
        firebaseRepo.obtenerConfiguracion { config ->
            if (config != null) {
                binding.etNombreNegocio.setText(config.nombreNegocio)
                binding.etRuc.setText(config.ruc)
                binding.etDireccion.setText(config.direccion)
                binding.etLinkQr.setText(config.linkQrNegocio)
                binding.etPorcentajeIgv.setText(config.porcentajeIGV.toString())

                if (config.logoUrl.isNotEmpty()) {
                    Glide.with(this).load(config.logoUrl).into(binding.ivLogoPreview)
                }
            }
        }
    }

    private fun guardarConfiguracion() {
        val nombre = binding.etNombreNegocio.text.toString()
        val ruc = binding.etRuc.text.toString()
        val direccion = binding.etDireccion.text.toString()
        val linkQr = binding.etLinkQr.text.toString()
        val igv = binding.etPorcentajeIgv.text.toString().toDoubleOrNull() ?: 18.0

        if (nombre.isNotEmpty()) {
            firebaseRepo.guardarConfiguracionCompleta(nombre, ruc, direccion, linkQr, igv) {
                Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "El nombre del negocio es requerido", Toast.LENGTH_SHORT).show()
        }
    }

    // ==================== FUNCIONES DE NOTIFICACIONES ====================

    private fun cargarNotificaciones() {
        firebaseRepo.obtenerNotificaciones { notificaciones ->
            if (notificaciones.isNotEmpty()) {
                // Mostrar solo la última notificación
                val ultima = notificaciones.first()
                val titulo = ultima["titulo"] as? String ?: ""
                val mensaje = ultima["mensaje"] as? String ?: ""
                mostrarNotificacionEnApp(titulo, mensaje)
            }
        }
    }

    private fun mostrarNotificacionEnApp(titulo: String, mensaje: String) {
        AlertDialog.Builder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setPositiveButton("OK") { _, _ ->
                // Aquí puedes agregar acción al hacer clic
            }
            .show()
    }
}