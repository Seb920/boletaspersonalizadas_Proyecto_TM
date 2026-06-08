package com.ejemplo.boletaspersonalizadas.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.ejemplo.boletaspersonalizadas.models.Boleta
import com.ejemplo.boletaspersonalizadas.models.ConfiguracionNegocio
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class GeneradorPDF {

    companion object {

        fun generarBoletaPDF(
            context: Context,
            boleta: Boleta,
            config: ConfiguracionNegocio
        ): String? {
            try {
                val directorio = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!directorio.exists()) {
                    directorio.mkdirs()
                }

                val archivo = File(directorio, "Boleta_${boleta.numeroSerie}.pdf")
                val writer = PdfWriter(archivo)
                val pdf = PdfDocument(writer)
                val document = Document(pdf, PageSize.A4)
                document.setMargins(50f, 50f, 50f, 50f)

                // === LOGO ===
                Log.d("PDF", "URL del logo: ${config.logoUrl}")

                if (config.logoUrl.isNotEmpty()) {
                    try {
                        val bitmap = cargarImagenDesdeUrl(config.logoUrl)
                        if (bitmap != null) {
                            val stream = java.io.ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            val data = stream.toByteArray()
                            val imgData = com.itextpdf.io.image.ImageDataFactory.create(data)
                            val image = Image(imgData).setWidth(80f).setHeight(80f)
                            image.setTextAlignment(TextAlignment.CENTER)
                            document.add(image)
                            Log.d("PDF", "Logo cargado exitosamente")
                        } else {
                            Log.e("PDF", "No se pudo cargar el bitmap")
                        }
                    } catch (e: Exception) {
                        Log.e("PDF", "Error al cargar logo: ${e.message}")
                    }
                }

                // === NOMBRE EMPRESA ===
                document.add(Paragraph(config.nombreNegocio)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(20f)
                    .setBold())

                document.add(Paragraph("RUC: ${config.ruc}")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10f))

                document.add(Paragraph(config.direccion)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10f))

                document.add(Paragraph(" "))

                // === TÍTULO ===
                document.add(Paragraph("BOLETA DE VENTA ELECTRÓNICA")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(14f)
                    .setBold())

                document.add(Paragraph("N° Serie: ${boleta.numeroSerie}")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(11f))

                val formatoFecha = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                val fechaStr = formatoFecha.format(Date(boleta.fecha))
                document.add(Paragraph("Fecha Emisión: $fechaStr")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10f))

                document.add(Paragraph(" "))

                // === DATOS CLIENTE ===
                document.add(Paragraph("DATOS DEL CLIENTE")
                    .setFontSize(12f)
                    .setBold())
                document.add(Paragraph("Señor(es): ${boleta.clienteNombre}"))
                document.add(Paragraph("Documento: ${boleta.clienteDocumento}"))
                document.add(Paragraph(" "))

                // === TABLA PRODUCTOS ===
                val tabla = Table(UnitValue.createPercentArray(floatArrayOf(10f, 45f, 15f, 15f, 15f)))
                tabla.setWidth(UnitValue.createPercentValue(100f))

                val headerCell = Cell().setBackgroundColor(ColorConstants.LIGHT_GRAY).setBold()
                tabla.addCell(headerCell.clone(true).add(Paragraph("Cant")))
                tabla.addCell(headerCell.clone(true).add(Paragraph("Producto")))
                tabla.addCell(headerCell.clone(true).add(Paragraph("P.Unit")))
                tabla.addCell(headerCell.clone(true).add(Paragraph("Total")))
                tabla.addCell(headerCell.clone(true).add(Paragraph("Código")))

                for (item in boleta.productos) {
                    tabla.addCell(Cell().add(Paragraph(item.cantidad.toString())))
                    tabla.addCell(Cell().add(Paragraph(item.nombre)))
                    tabla.addCell(Cell().add(Paragraph("S/ ${String.format("%.2f", item.precioUnitario)}")))
                    tabla.addCell(Cell().add(Paragraph("S/ ${String.format("%.2f}", item.total)}")))
                    tabla.addCell(Cell().add(Paragraph(item.codigo)))
                }

                document.add(tabla)
                document.add(Paragraph(" "))

                // === TOTALES ===
                document.add(Paragraph("OP. GRAVADA: S/ ${String.format("%.2f", boleta.subtotal)}")
                    .setTextAlignment(TextAlignment.RIGHT))
                document.add(Paragraph("IGV (${String.format("%.1f", config.porcentajeIGV)}%): S/ ${String.format("%.2f", boleta.igv)}")
                    .setTextAlignment(TextAlignment.RIGHT))
                document.add(Paragraph("TOTAL: S/ ${String.format("%.2f", boleta.total)}")
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBold()
                    .setFontSize(14f))

                document.add(Paragraph(" "))
                document.add(Paragraph(" "))

                // === QR CENTRADO ===
                try {
                    val qrText = if (config.linkQrNegocio.isNotEmpty()) {
                        config.linkQrNegocio
                    } else {
                        "https://${config.nombreNegocio.replace(" ", "").lowercase()}.com"
                    }

                    val qrCode = generarQRCode(qrText, 200, 200)
                    if (qrCode != null) {
                        val stream = java.io.ByteArrayOutputStream()
                        qrCode.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        val data = stream.toByteArray()
                        val imgData = com.itextpdf.io.image.ImageDataFactory.create(data)
                        val qrImage = Image(imgData).setWidth(120f).setHeight(120f)

                        val qrParagraph = Paragraph()
                            .setTextAlignment(TextAlignment.CENTER)
                            .add(qrImage)
                        document.add(qrParagraph)

                        document.add(Paragraph("Escanee el código QR para más información")
                            .setTextAlignment(TextAlignment.CENTER)
                            .setFontSize(9f)
                            .setFontColor(ColorConstants.BLUE))
                    }
                } catch (e: Exception) {
                    Log.e("PDF", "Error al generar QR: ${e.message}")
                }

                document.add(Paragraph(" "))
                document.add(Paragraph(" "))

                // === PIE ===
                document.add(Paragraph("Gracias por su compra")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(11f)
                    .setBold())

                document.add(Paragraph("Este documento es una representación impresa de la Boleta Electrónica")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(8f))

                document.close()

                Toast.makeText(context, "✅ Boleta guardada en: Download/Boleta_${boleta.numeroSerie}.pdf", Toast.LENGTH_LONG).show()

                return archivo.absolutePath

            } catch (e: Exception) {
                Log.e("PDF", "Error general: ${e.message}")
                Toast.makeText(context, "Error al generar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                return null
            }
        }

        private fun cargarImagenDesdeUrl(url: String): Bitmap? {
            return try {
                if (url.startsWith("http")) {
                    val conexion = URL(url).openConnection()
                    conexion.connect()
                    val inputStream = conexion.getInputStream()
                    BitmapFactory.decodeStream(inputStream)
                } else {
                    BitmapFactory.decodeFile(url)
                }
            } catch (e: Exception) {
                Log.e("PDF", "Error cargando imagen: ${e.message}")
                null
            }
        }

        private fun generarQRCode(text: String, width: Int, height: Int): Bitmap? {
            return try {
                val matrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height)
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                    }
                }
                bmp
            } catch (e: Exception) {
                Log.e("PDF", "Error generando QR: ${e.message}")
                null
            }
        }
    }
}