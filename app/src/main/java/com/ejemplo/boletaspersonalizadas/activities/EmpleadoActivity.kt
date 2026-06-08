package com.ejemplo.boletaspersonalizadas.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.ejemplo.boletaspersonalizadas.R
import com.ejemplo.boletaspersonalizadas.adapters.ProductoAdapter
import com.ejemplo.boletaspersonalizadas.databinding.ActivityEmpleadoBinding
import com.ejemplo.boletaspersonalizadas.models.ItemBoleta
import com.ejemplo.boletaspersonalizadas.models.Producto
import com.ejemplo.boletaspersonalizadas.models.Boleta
import com.ejemplo.boletaspersonalizadas.repositories.FirebaseRepository
import com.ejemplo.boletaspersonalizadas.utils.PreferenciasManager
import com.ejemplo.boletaspersonalizadas.utils.GeneradorPDF
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class EmpleadoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmpleadoBinding
    private lateinit var firebaseRepo: FirebaseRepository
    private lateinit var preferencias: PreferenciasManager
    private lateinit var adapter: ProductoAdapter
    private val carrito = mutableListOf<ItemBoleta>()
    private var numeroBoletaCorrelativo = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEmpleadoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseRepo = FirebaseRepository()
        preferencias = PreferenciasManager(this)

        cargarUltimoNumeroBoleta()
        configurarRecyclerView()
        cargarProductos()

        binding.btnAgregarProducto.setOnClickListener {
            val codigo = binding.etCodigoProducto.text.toString()
            if (codigo.isNotEmpty()) {
                agregarProductoPorCodigo(codigo)
            } else {
                Toast.makeText(this, "Ingrese un código de producto", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnGenerarBoleta.setOnClickListener {
            if (carrito.isNotEmpty()) {
                mostrarDialogoCliente()
            } else {
                Toast.makeText(this, "Agregue productos al carrito primero", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            preferencias.limpiarSesion()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnLimpiarCarrito.setOnClickListener {
            carrito.clear()
            actualizarCarrito()
            Toast.makeText(this, "Carrito limpiado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cargarUltimoNumeroBoleta() {
        val prefs = getSharedPreferences("boletas_prefs", MODE_PRIVATE)
        numeroBoletaCorrelativo = prefs.getInt("ultimo_numero_boleta", 1)
    }

    private fun guardarUltimoNumeroBoleta() {
        val prefs = getSharedPreferences("boletas_prefs", MODE_PRIVATE)
        prefs.edit().putInt("ultimo_numero_boleta", numeroBoletaCorrelativo + 1).apply()
    }

    private fun generarNumeroBoleta(): String {
        val numeroActual = numeroBoletaCorrelativo
        return String.format("B%06d", numeroActual)
    }

    private fun configurarRecyclerView() {
        adapter = ProductoAdapter { producto, cantidad ->
            agregarAlCarrito(producto, cantidad)
        }
        binding.rvProductos.layoutManager = LinearLayoutManager(this)
        binding.rvProductos.adapter = adapter
    }

    private fun cargarProductos() {
        firebaseRepo.obtenerProductos { productos ->
            if (productos.isEmpty()) {
                Toast.makeText(this, "No hay productos disponibles", Toast.LENGTH_LONG).show()
            }
            adapter.actualizarLista(productos)
        }
    }

    private fun agregarProductoPorCodigo(codigo: String) {
        firebaseRepo.obtenerProductos { productos ->
            val producto = productos.find { it.codigo == codigo }
            if (producto != null) {
                mostrarDialogoCantidad(producto)
                binding.etCodigoProducto.text?.clear()
            } else {
                Toast.makeText(this, "Producto no encontrado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoCantidad(producto: Producto) {
        val input = TextInputEditText(this)
        input.hint = "Cantidad"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.setText("1")

        AlertDialog.Builder(this)
            .setTitle("Cantidad de ${producto.nombre}")
            .setView(input)
            .setPositiveButton("Agregar") { _, _ ->
                val cantidad = input.text.toString().toIntOrNull() ?: 1
                if (cantidad > 0) {
                    agregarAlCarrito(producto, cantidad)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun agregarAlCarrito(producto: Producto, cantidad: Int) {
        val item = ItemBoleta(
            codigo = producto.codigo,
            nombre = producto.nombre,
            cantidad = cantidad,
            precioUnitario = producto.precioUnitario,
            total = producto.precioUnitario * cantidad
        )

        val existente = carrito.find { it.codigo == producto.codigo }
        if (existente != null) {
            val nuevoItem = existente.copy(
                cantidad = existente.cantidad + cantidad,
                total = (existente.cantidad + cantidad) * existente.precioUnitario
            )
            carrito.remove(existente)
            carrito.add(nuevoItem)
        } else {
            carrito.add(item)
        }

        actualizarCarrito()
        Toast.makeText(this, "Agregado: ${producto.nombre} x$cantidad", Toast.LENGTH_SHORT).show()
    }

    private fun actualizarCarrito() {
        val texto = carrito.joinToString("\n") {
            "${it.nombre} x${it.cantidad} = S/ ${String.format("%.2f", it.total)}"
        }
        binding.tvCarrito.text = if (carrito.isEmpty()) "Carrito vacío" else texto

        val total = carrito.sumOf { it.total }
        binding.tvTotal.text = "Total: S/ ${String.format("%.2f", total)}"
    }

    private fun mostrarDialogoCliente() {
        val view = layoutInflater.inflate(R.layout.dialog_cliente, null)
        val etNombre = view.findViewById<TextInputEditText>(R.id.etClienteNombre)
        val etDocumento = view.findViewById<TextInputEditText>(R.id.etClienteDocumento)

        AlertDialog.Builder(this)
            .setTitle("Datos del Cliente")
            .setView(view)
            .setPositiveButton("Generar Boleta") { _, _ ->
                val nombre = etNombre.text.toString()
                val documento = etDocumento.text.toString()

                if (nombre.isNotEmpty() && documento.isNotEmpty()) {
                    generarBoleta(nombre, documento)
                } else {
                    Toast.makeText(this, "Complete los datos del cliente", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun generarBoleta(clienteNombre: String, clienteDocumento: String) {
        firebaseRepo.obtenerConfiguracion { config ->
            if (config == null) {
                Toast.makeText(this, "Error: Configuración no encontrada", Toast.LENGTH_SHORT).show()
                return@obtenerConfiguracion
            }

            val subtotal = carrito.sumOf { it.total }
            val igv = subtotal * (config.porcentajeIGV / 100)
            val total = subtotal + igv
            val numeroSerie = generarNumeroBoleta()

            val boleta = Boleta(
                numeroSerie = numeroSerie,
                productos = carrito.toList(),
                subtotal = subtotal,
                igv = igv,
                total = total,
                clienteNombre = clienteNombre,
                clienteDocumento = clienteDocumento,
                empleadoId = preferencias.getUserId(),
                empleadoNombre = preferencias.getNombre(),
                linkQr = config.linkQrNegocio
            )

            firebaseRepo.guardarBoleta(boleta) { boletaId ->
                val pdfPath = GeneradorPDF.generarBoletaPDF(
                    this, boleta, config
                )

                if (pdfPath != null) {
                    guardarUltimoNumeroBoleta()
                    numeroBoletaCorrelativo++

                    Toast.makeText(this, "✅ Boleta $numeroSerie generada", Toast.LENGTH_LONG).show()
                    carrito.clear()
                    actualizarCarrito()
                } else {
                    Toast.makeText(this, "Error al generar el PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}