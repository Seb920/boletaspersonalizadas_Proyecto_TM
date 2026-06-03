package com.ejemplo.boletaspersonalizadas.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ejemplo.boletaspersonalizadas.databinding.ItemProductoBinding
import com.ejemplo.boletaspersonalizadas.models.Producto

class ProductoAdapter(
    private val onAgregarClick: (Producto, Int) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ViewHolder>() {

    private var productos = listOf<Producto>()

    fun actualizarLista(nuevaLista: List<Producto>) {
        productos = nuevaLista
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(productos[position], onAgregarClick)
    }

    override fun getItemCount(): Int = productos.size

    class ViewHolder(private val binding: ItemProductoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(producto: Producto, onAgregarClick: (Producto, Int) -> Unit) {
            binding.tvNombre.text = producto.nombre
            binding.tvCodigo.text = producto.codigo
            binding.tvPrecio.text = "S/ ${"%.2f".format(producto.precioUnitario)}"

            binding.btnAgregar.setOnClickListener {
                val cantidad = binding.etCantidad.text.toString().toIntOrNull() ?: 1
                onAgregarClick(producto, cantidad)
                binding.etCantidad.setText("1")
            }
        }
    }
}