package com.frychanel.duokeyboard.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

data class LugarSatelital(val nombre: String, val lat: Double, val lon: Double, val zoom: Int)

/**
 * Guarda los fondos del teclado en el almacenamiento interno de la app.
 * Se copian las imágenes (en vez de guardar el Uri) porque el teclado
 * no conserva permisos sobre archivos de la galería.
 */
object BackgroundStore {

    val LUGARES = listOf(
        LugarSatelital("Ciudad de México", 19.4326, -99.1332, 13),
        LugarSatelital("Chichén Itzá", 20.6829, -88.5686, 17),
        LugarSatelital("Gran Cañón", 36.1069, -112.1129, 12),
        LugarSatelital("Monte Fuji", 35.3606, 138.7274, 12),
        LugarSatelital("Everest", 27.9881, 86.9250, 12),
        LugarSatelital("Sahara", 23.4162, 25.6628, 9),
        LugarSatelital("Gran Barrera de Coral", -18.2871, 147.6992, 10),
        LugarSatelital("Maldivas", 4.1755, 73.5093, 12),
        LugarSatelital("Ámsterdam", 52.3676, 4.9041, 14),
        LugarSatelital("Islandia", 64.40, -16.80, 9),
    )

    fun archivoGaleria(ctx: Context) = File(ctx.filesDir, "fondo_galeria.jpg")
    fun archivoSatelite(ctx: Context) = File(ctx.filesDir, "fondo_satelite.jpg")

    /** Carga el fondo listo para pintar (reducido para no gastar memoria). */
    fun cargar(ctx: Context, tipo: BackgroundType): ImageBitmap? {
        val f = when (tipo) {
            BackgroundType.TEMA -> return null
            BackgroundType.GALERIA -> archivoGaleria(ctx)
            BackgroundType.SATELITE -> archivoSatelite(ctx)
        }
        if (!f.exists()) return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.absolutePath, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= 1200) sample *= 2
            BitmapFactory.decodeFile(f.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
                ?.asImageBitmap()
        }.getOrNull()
    }

    suspend fun importarDeGaleria(ctx: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val bmp: Bitmap = if (Build.VERSION.SDK_INT >= 28) {
                val fuente = ImageDecoder.createSource(ctx.contentResolver, uri)
                ImageDecoder.decodeBitmap(fuente) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val w = info.size.width
                    val h = info.size.height
                    val escala = min(1f, 1600f / max(w, h))
                    decoder.setTargetSize((w * escala).toInt().coerceAtLeast(1), (h * escala).toInt().coerceAtLeast(1))
                }
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var s = 1
                while (max(bounds.outWidth, bounds.outHeight) / (s * 2) >= 1600) s *= 2
                ctx.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = s })
                } ?: error("No se pudo leer la imagen")
            }
            FileOutputStream(archivoGaleria(ctx)).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            true
        }.getOrDefault(false)
    }

    /**
     * Descarga un mosaico de 4x2 teselas satelitales (1024x512 px) centrado en
     * lat/lon y lo guarda como fondo. Fuente: Esri World Imagery.
     */
    suspend fun descargarSatelite(ctx: Context, lat: Double, lon: Double, zoom: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val z = zoom.coerceIn(2, 18)
                val n = 1 shl z
                val latR = Math.toRadians(lat.coerceIn(-85.0, 85.0))
                val xf = (lon + 180.0) / 360.0 * n
                val yf = (1.0 - ln(tan(latR) + 1.0 / cos(latR)) / PI) / 2.0 * n
                val cols = 4
                val filas = 2
                val x0 = floor(xf - cols / 2.0).toInt()
                val y0 = floor(yf - filas / 2.0).toInt()

                val salida = Bitmap.createBitmap(cols * 256, filas * 256, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(salida)
                for (r in 0 until filas) for (c in 0 until cols) {
                    val x = ((x0 + c) % n + n) % n
                    val y = (y0 + r).coerceIn(0, n - 1)
                    val url = URL("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$z/$y/$x")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 10_000
                        readTimeout = 10_000
                        setRequestProperty("User-Agent", "DuoKeyboard/1.0 (Android)")
                    }
                    try {
                        conn.inputStream.use { stream ->
                            val tesela = BitmapFactory.decodeStream(stream) ?: error("Tesela vacía")
                            canvas.drawBitmap(tesela, c * 256f, r * 256f, null)
                            tesela.recycle()
                        }
                    } finally {
                        conn.disconnect()
                    }
                }
                FileOutputStream(archivoSatelite(ctx)).use { salida.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                salida.recycle()
            }
        }
}
