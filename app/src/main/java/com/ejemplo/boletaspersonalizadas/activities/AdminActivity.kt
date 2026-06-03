package com.ejemplo.boletaspersonalizadas.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ejemplo.boletaspersonalizadas.databinding.ActivityAdminBinding
import com.ejemplo.boletaspersonalizadas.models.Producto
import com.ejemplo.boletaspersonalizadas.repositories.FirebaseRepository
import com.ejemplo.boletaspersonalizadas.utils.PreferenciasManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.bumptech.glide.Glide

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

        // Configurar botones de productos
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
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(com.ejemplo.boletaspersonalizadas.R.layout.dialog_producto, null)

        val etCodigo = view.findViewById<com.google.android.material.textfield.TextInputEditText>(com.ejemplo.boletaspersonalizadas.R.id.etCodigo)
        val etNombre = view.findViewById<com.google.android.material.textfield.TextInputEditText>(com.ejemplo.boletaspersonalizadas.R.id.etNombre)
        val etPrecio = view.findViewById<com.google.android.material.textfield.TextInputEditText>(com.ejemplo.boletaspersonalizadas.R.id.etPrecio)

        if (producto != null) {
            etCodigo.setText(producto.codigo)
            etNombre.setText(producto.nombre)
            etPrecio.setText(producto.precioUnitario.toString())
        }

        builder.setTitle(if (producto == null) "Agregar Producto" else "Editar Producto")
        builder.setView(view)
        builder.setPositiveButton("Guardar") { _, _ ->
            val nuevoProducto = Producto(
                id = producto?.id ?: "",
                codigo = etCodigo.text.toString(),
                nombre = etNombre.text.toString(),
                precioUnitario = etPrecio.text.toString().toDoubleOrNull() ?: 0.0
            )

            if (producto == null) {
                firebaseRepo.agregarProducto(nuevoProducto) {
                    cargarProductos()
                    Toast.makeText(this, "Producto agregado", Toast.LENGTH_SHORT).show()
                }
            } else {
                firebaseRepo.actualizarProducto(nuevoProducto) {
                    cargarProductos()
                    Toast.makeText(this, "Producto actualizado", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun cargarProductos() {
        firebaseRepo.obtenerProductos { productos ->
            val texto = productos.joinToString("\n") {
                "${it.codigo} - ${it.nombre} - S/ ${it.precioUnitario}"
            }
            binding.tvListaProductos.text = if (productos.isEmpty()) "No hay productos" else texto
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
        val linkQr = binding.etLinkQr.text.toString()
        val igv = binding.etPorcentajeIgv.text.toString().toDoubleOrNull() ?: 18.0

        firebaseRepo.guardarConfiguracion(nombre, linkQr, igv) {
            Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show()
        }
    }
}