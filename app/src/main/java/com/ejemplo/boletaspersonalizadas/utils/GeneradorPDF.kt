package com.ejemplo.boletaspersonalizadas.utils

import android.content.Context
import android.graphics.BitmapFactory
import com.bumptech.glide.Glide
import com.ejemplo.boletaspersonalizadas.models.Boleta
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.*

class GeneradorPDF {

    companion object {
        fun generarBoletaPDF(
            context: Context,
            boleta: Boleta,
            nombreNegocio: String,
            logoUrl: String
        ): String? {
            try {
                val directorio = context.getExternalFilesDir(null)
                val archivo = File(directorio, "boleta_${boleta.numeroSerie}.pdf")
                val writer = PdfWriter(archivo)
                val pdf = PdfDocument(writer)
                val document = Document(pdf, PageSize.A4)

                // Logo y encabezado
                if (logoUrl.isNotEmpty()) {
                    try {
                        val bitmap = Glide.with(context)
                            .asBitmap()
                            .load(logoUrl)
                            .submit()
                            .get()

                        val stream = java.io.ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        val data = stream.toByteArray()
                        val imgData = com.itextpdf.io.image.ImageDataFactory.create(data)
                        val image = Image(imgData).setWidth(100f).setHeight(100f)
                        image.setTextAlignment(TextAlignment.CENTER)
                        document.add(image)
                    } catch (e: Exception) {
                        // Si no se puede cargar el logo, continuar sin él
                    }
                }

                // Título
                document.add(Paragraph(nombreNegocio)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(18f)
                    .setBold())

                document.add(Paragraph("BOLETA DE VENTA ELECTRÓNICA")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(14f))

                document.add(Paragraph("N° Serie: ${boleta.numeroSerie}")
                    .setTextAlignment(TextAlignment.CENTER))

                document.add(Paragraph("Fecha: ${android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", boleta.fecha)}")
                    .setTextAlignment(TextAlignment.CENTER))

                document.add(Paragraph(" "))

                // Datos del cliente
                document.add(Paragraph("DATOS DEL CLIENTE")
                    .setFontSize(12f)
                    .setBold())
                document.add(Paragraph("Cliente: ${boleta.clienteNombre}"))
                document.add(Paragraph("Documento: ${boleta.clienteDocumento}"))

                document.add(Paragraph(" "))

                // Tabla de productos
                val tabla = Table(UnitValue.createPercentArray(floatArrayOf(10f, 40f, 15f, 15f, 20f)))
                tabla.setWidth(UnitValue.createPercentValue(100f))

                // Encabezados
                tabla.addCell(Cell().add(Paragraph("Cant")).setBackgroundColor(ColorConstants.LIGHT_GRAY))
                tabla.addCell(Cell().add(Paragraph("Producto")).setBackgroundColor(ColorConstants.LIGHT_GRAY))
                tabla.addCell(Cell().add(Paragraph("P.Unit")).setBackgroundColor(ColorConstants.LIGHT_GRAY))
                tabla.addCell(Cell().add(Paragraph("Total")).setBackgroundColor(ColorConstants.LIGHT_GRAY))
                tabla.addCell(Cell().add(Paragraph("Código")).setBackgroundColor(ColorConstants.LIGHT_GRAY))

                // Datos
                for (item in boleta.productos) {
                    tabla.addCell(Cell().add(Paragraph(item.cantidad.toString())))
                    tabla.addCell(Cell().add(Paragraph(item.nombre)))
                    tabla.addCell(Cell().add(Paragraph("S/ ${"%.2f".format(item.precioUnitario)}")))
                    tabla.addCell(Cell().add(Paragraph("S/ ${"%.2f".format(item.total)}")))
                    tabla.addCell(Cell().add(Paragraph(item.codigo)))
                }

                document.add(tabla)

                document.add(Paragraph(" "))

                // Totales
                document.add(Paragraph("SUBTOTAL: S/ ${"%.2f".format(boleta.subtotal)}")
                    .setTextAlignment(TextAlignment.RIGHT))
                document.add(Paragraph("IGV (18%): S/ ${"%.2f".format(boleta.igv)}")
                    .setTextAlignment(TextAlignment.RIGHT))
                document.add(Paragraph("TOTAL: S/ ${"%.2f".format(boleta.total)}")
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBold()
                    .setFontSize(14f))

                document.add(Paragraph(" "))

                // QR Code
                try {
                    val qrCode = generarQRCode(boleta.linkQr, 200, 200)
                    if (qrCode != null) {
                        val stream = java.io.ByteArrayOutputStream()
                        qrCode.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        val data = stream.toByteArray()
                        val imgData = com.itextpdf.io.image.ImageDataFactory.create(data)
                        val qrImage = Image(imgData).setWidth(100f).setHeight(100f)
                        qrImage.setTextAlignment(TextAlignment.CENTER)
                        document.add(qrImage)
                        document.add(Paragraph("Escanee para más información")
                            .setTextAlignment(TextAlignment.CENTER)
                            .setFontSize(10f))
                    }
                } catch (e: Exception) {
                    document.add(Paragraph("Link: ${boleta.linkQr}")
                        .setTextAlignment(TextAlignment.CENTER))
                }

                document.add(Paragraph(" "))
                document.add(Paragraph("Gracias por su compra")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10f))

                document.close()

                return archivo.absolutePath

            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        private fun generarQRCode(text: String, width: Int, height: Int): Bitmap? {
            try {
                val matrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height)
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                    }
                }
                return bmp
            } catch (e: Exception) {
                return null
            }
        }
    }
}