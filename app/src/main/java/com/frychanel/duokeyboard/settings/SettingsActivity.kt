package com.frychanel.duokeyboard.settings

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.frychanel.duokeyboard.data.BackgroundStore
import com.frychanel.duokeyboard.data.BackgroundType
import com.frychanel.duokeyboard.data.ConfigRepository
import com.frychanel.duokeyboard.data.KeyStyle
import com.frychanel.duokeyboard.data.KeyboardConfig
import com.frychanel.duokeyboard.data.LayoutMode
import com.frychanel.duokeyboard.data.ThemeId
import com.frychanel.duokeyboard.keyboard.DuoKeyboardView
import com.frychanel.duokeyboard.keyboard.KeyboardController
import com.frychanel.duokeyboard.keyboard.themeFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ConfigRepository.init(this)
        enableEdgeToEdge()
        setContent { TemaAjustes { PantallaAjustes() } }
    }
}

@Composable
private fun TemaAjustes(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF0A84FF),
            onPrimary = Color.White,
            secondaryContainer = Color(0xFF0A3D7A),
            onSecondaryContainer = Color.White,
            background = Color.Black,
            onBackground = Color.White,
            surface = Color(0xFF1C1C1E),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF2C2C2E),
            onSurfaceVariant = Color(0xFFAEAEB2),
            outline = Color(0xFF48484A),
        ),
        content = content,
    )
}

private data class EstadoTeclado(val habilitado: Boolean, val seleccionado: Boolean) {
    val listo get() = habilitado && seleccionado

    companion object {
        fun leer(ctx: Context): EstadoTeclado {
            val imm = ctx.getSystemService(InputMethodManager::class.java)
            val habilitado = imm.enabledInputMethodList.any { it.packageName == ctx.packageName }
            val actual = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: ""
            return EstadoTeclado(habilitado, actual.startsWith(ctx.packageName + "/"))
        }
    }
}

private enum class Seccion(val titulo: String) {
    ACTIVAR("Activar"), DISENO("Diseño"), ESTILO("Estilo"), FONDO("Fondo"), TECLAS("Teclas"), ESCRITURA("Escritura")
}

@Composable
private fun PantallaAjustes() {
    val ctx = LocalContext.current
    val config by ConfigRepository.config.collectAsStateWithLifecycle()
    var estado by remember { mutableStateOf(EstadoTeclado.leer(ctx)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { estado = EstadoTeclado.leer(ctx) }
    LaunchedEffect(estado.listo) {
        while (!estado.listo) { delay(1000); estado = EstadoTeclado.leer(ctx) }
    }

    var seccion by rememberSaveable { mutableStateOf(if (estado.listo) Seccion.DISENO else Seccion.ACTIVAR) }
    val vistaPrevia = remember { KeyboardController(ctx, { null }, { null }, esVistaPrevia = true) }
    val fondo by produceState<ImageBitmap?>(null, config.fondoTipo, config.fondoVersion) {
        value = withContext(Dispatchers.IO) { BackgroundStore.cargar(ctx, config.fondoTipo) }
    }
    val horizontal = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(containerColor = Color.Black) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Encabezado(estado)
            Box(Modifier.padding(horizontal = 12.dp).clip(RoundedCornerShape(22.dp))) {
                DuoKeyboardView(vistaPrevia, config, fondo, horizontal, respetarBarraNavegacion = false)
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Seccion.entries.forEach {
                    FilterChip(selected = it == seccion, onClick = { seccion = it }, label = { Text(it.titulo) })
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (seccion) {
                    Seccion.ACTIVAR -> SeccionActivar(estado)
                    Seccion.DISENO -> SeccionDiseno(config)
                    Seccion.ESTILO -> SeccionEstilo(config)
                    Seccion.FONDO -> SeccionFondo(config)
                    Seccion.TECLAS -> SeccionTeclas(config)
                    Seccion.ESCRITURA -> SeccionEscritura(config)
                }
            }
        }
    }
}

@Composable
private fun Encabezado(estado: EstadoTeclado) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("DuoKeyboard", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Tu teclado, a tu manera", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val (texto, color) = if (estado.listo) "Activo" to Color(0xFF30D158) else "Sin activar" to Color(0xFFFF9F0A)
        Text(
            texto,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

private fun pct(v: Float) = "${(v * 100).roundToInt()}%"

// ------------------------------------------------------------------
// Secciones
// ------------------------------------------------------------------

@Composable
private fun SeccionActivar(estado: EstadoTeclado) {
    val ctx = LocalContext.current
    Tarjeta("1 · Habilitar DuoKeyboard") {
        Text("Actívalo en la lista de teclados de Android. Android mostrará un aviso estándar para teclados de terceros.")
        Button(onClick = {
            ctx.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }) { Text(if (estado.habilitado) "Habilitado ✓" else "Abrir ajustes de teclado") }
    }
    Tarjeta("2 · Elegirlo como teclado") {
        Button(
            onClick = { ctx.getSystemService(InputMethodManager::class.java).showInputMethodPicker() },
            enabled = estado.habilitado,
        ) { Text(if (estado.seleccionado) "Seleccionado ✓" else "Elegir teclado") }
    }
    Tarjeta("3 · Pruébalo") {
        var texto by rememberSaveable { mutableStateOf("") }
        OutlinedTextField(
            value = texto,
            onValueChange = { texto = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
            placeholder = { Text("Escribe aquí…") },
        )
    }
    Tarjeta("Privacidad") {
        Text(
            "DuoKeyboard no guarda ni envía lo que escribes. Solo usa internet para descargar " +
                "imágenes satelitales cuando tú lo pides.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SeccionDiseno(c: KeyboardConfig) {
    Tarjeta("Modo del teclado") {
        Chips(LayoutMode.entries, c.modoTeclado, { it.etiqueta }) { m -> ConfigRepository.update { it.copy(modoTeclado = m) } }
        Interruptor("Duo automático en horizontal", "Al girar el teléfono se divide solo", c.duoEnHorizontal) { v ->
            ConfigRepository.update { it.copy(duoEnHorizontal = v) }
        }
    }
    Tarjeta("Duo · dividido") {
        Deslizador("Separación central", c.separacionDuo, 0.04f..0.40f, ::pct) { v -> ConfigRepository.update { it.copy(separacionDuo = v) } }
        Deslizador("Inclinación de las mitades", c.anguloDuo, 0f..15f, { "${it.roundToInt()}°" }) { v ->
            ConfigRepository.update { it.copy(anguloDuo = v) }
        }
        Deslizador("Curvatura (arco del pulgar)", c.curva, 0f..24f, { "${it.roundToInt()} dp" }) { v ->
            ConfigRepository.update { it.copy(curva = v) }
        }
    }
    Tarjeta("Tamaño y posición") {
        Deslizador("Altura", c.alturaEscala, 0.7f..1.45f, ::pct) { v -> ConfigRepository.update { it.copy(alturaEscala = v) } }
        Deslizador("Ancho en una mano", c.anchoUnaMano, 0.6f..0.95f, ::pct) { v -> ConfigRepository.update { it.copy(anchoUnaMano = v) } }
        Deslizador("Elevar desde abajo", c.elevacion, 0f..60f, { "${it.roundToInt()} dp" }) { v ->
            ConfigRepository.update { it.copy(elevacion = v) }
        }
        Interruptor("Fila de números", activo = c.filaNumeros) { v -> ConfigRepository.update { it.copy(filaNumeros = v) } }
        Interruptor("Barra de herramientas", "Ajustes, modo, tema y ocultar fondo", c.barraHerramientas) { v ->
            ConfigRepository.update { it.copy(barraHerramientas = v) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeccionEstilo(c: KeyboardConfig) {
    Tarjeta("Tema") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ThemeId.entries.forEach { id ->
                MiniaturaTema(c, id, c.tema == id) { ConfigRepository.update { it.copy(tema = id) } }
            }
        }
    }
    Tarjeta("Estilo de tecla") {
        Chips(KeyStyle.entries, c.estiloTecla, { it.etiqueta }) { s -> ConfigRepository.update { it.copy(estiloTecla = s) } }
    }
    if (c.tema == ThemeId.PERSONAL) {
        Tarjeta("Colores personalizados") {
            SelectorColor("Fondo", c.colorFondo) { v -> ConfigRepository.update { it.copy(colorFondo = v) } }
            SelectorColor("Teclas", c.colorTecla) { v -> ConfigRepository.update { it.copy(colorTecla = v) } }
            SelectorColor("Texto", c.colorTexto) { v -> ConfigRepository.update { it.copy(colorTexto = v) } }
            SelectorColor("Acento", c.colorAcento) { v -> ConfigRepository.update { it.copy(colorAcento = v) } }
        }
    } else {
        Text(
            "Elige «Personalizado» para escoger tus propios colores.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MiniaturaTema(c: KeyboardConfig, id: ThemeId, elegido: Boolean, onClick: () -> Unit) {
    val t = remember(c, id) { themeFor(c, id) }
    Column(Modifier.width(96.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(t.fondo)
                .border(
                    if (elegido) 2.5.dp else 1.dp,
                    if (elegido) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f),
                    RoundedCornerShape(12.dp),
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(t.tecla, t.tecla, t.especial, t.acento).forEach { col ->
                    Box(Modifier.size(width = 16.dp, height = 22.dp).clip(RoundedCornerShape(4.dp)).background(col))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(id.etiqueta, fontSize = 12.sp, color = if (elegido) MaterialTheme.colorScheme.primary else Color.White)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeccionFondo(c: KeyboardConfig) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var descargando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var latTxt by rememberSaveable { mutableStateOf("19.4326") }
    var lonTxt by rememberSaveable { mutableStateOf("-99.1332") }
    var zoom by remember { mutableFloatStateOf(13f) }

    val elegirFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            mensaje = "Procesando foto…"
            if (BackgroundStore.importarDeGaleria(ctx, uri)) {
                ConfigRepository.update { it.copy(fondoTipo = BackgroundType.GALERIA, fondoOculto = false, fondoVersion = it.fondoVersion + 1) }
                mensaje = "Foto aplicada ✓"
            } else {
                mensaje = "No se pudo usar esa imagen."
            }
        }
    }

    fun descargar(nombre: String, lat: Double, lon: Double, z: Int) {
        if (descargando) return
        scope.launch {
            descargando = true
            mensaje = "Descargando $nombre…"
            BackgroundStore.descargarSatelite(ctx, lat, lon, z)
                .onSuccess {
                    ConfigRepository.update {
                        it.copy(fondoTipo = BackgroundType.SATELITE, sateliteNombre = nombre, fondoOculto = false, fondoVersion = it.fondoVersion + 1)
                    }
                    mensaje = "$nombre aplicado ✓"
                }
                .onFailure { mensaje = "No se pudo descargar. Revisa tu conexión." }
            descargando = false
        }
    }

    Tarjeta("Tipo de fondo") {
        Chips(BackgroundType.entries, c.fondoTipo, { it.etiqueta }) { t -> ConfigRepository.update { it.copy(fondoTipo = t) } }
        Interruptor("Esconder fondo", "Oculta la imagen sin borrarla (también desde la barra del teclado)", c.fondoOculto) { v ->
            ConfigRepository.update { it.copy(fondoOculto = v) }
        }
    }
    Tarjeta("Foto de tu galería") {
        Button(onClick = { elegirFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
            Text("Elegir foto")
        }
    }
    Tarjeta("Imágenes satelitales") {
        Text("Pon cualquier lugar del planeta como fondo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BackgroundStore.LUGARES.forEach { lugar ->
                FilterChip(
                    selected = c.fondoTipo == BackgroundType.SATELITE && c.sateliteNombre == lugar.nombre,
                    onClick = { descargar(lugar.nombre, lugar.lat, lugar.lon, lugar.zoom) },
                    label = { Text(lugar.nombre) },
                    enabled = !descargando,
                )
            }
        }
        Text("O usa tus coordenadas", fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                latTxt, { latTxt = it }, Modifier.weight(1f),
                label = { Text("Latitud") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                lonTxt, { lonTxt = it }, Modifier.weight(1f),
                label = { Text("Longitud") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        Deslizador("Zoom", zoom, 3f..18f, { it.roundToInt().toString() }) { zoom = it }
        OutlinedButton(
            enabled = !descargando,
            onClick = {
                val lat = latTxt.replace(',', '.').toDoubleOrNull()
                val lon = lonTxt.replace(',', '.').toDoubleOrNull()
                if (lat == null || lon == null || lat !in -85.0..85.0 || lon !in -180.0..180.0) {
                    mensaje = "Coordenadas no válidas."
                } else {
                    descargar("Mi lugar", lat, lon, zoom.roundToInt())
                }
            },
        ) { Text("Descargar este lugar") }
        if (descargando) LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(
            "Imágenes: Esri, Maxar, Earthstar Geographics y la comunidad de usuarios GIS.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    mensaje?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    Tarjeta("Ajustes de imagen") {
        Deslizador("Oscurecer", c.fondoOscurecer, 0f..0.8f, ::pct) { v -> ConfigRepository.update { it.copy(fondoOscurecer = v) } }
        Deslizador("Desenfoque (Android 12+)", c.fondoDesenfoque, 0f..25f, { "${it.roundToInt()}" }) { v ->
            ConfigRepository.update { it.copy(fondoDesenfoque = v) }
        }
    }
}

@Composable
private fun SeccionTeclas(c: KeyboardConfig) {
    Tarjeta("Forma") {
        Deslizador("Redondeo de esquinas", c.radioTecla, 0f..22f, { "${it.roundToInt()} dp" }) { v -> ConfigRepository.update { it.copy(radioTecla = v) } }
        Deslizador("Espacio entre teclas", c.espaciado, 2f..14f, { "${it.roundToInt()} dp" }) { v -> ConfigRepository.update { it.copy(espaciado = v) } }
        Deslizador("Opacidad de teclas", c.opacidadTecla, 0.2f..1f, ::pct) { v -> ConfigRepository.update { it.copy(opacidadTecla = v) } }
        Deslizador("Tamaño de letra", c.escalaTexto, 0.8f..1.3f, ::pct) { v -> ConfigRepository.update { it.copy(escalaTexto = v) } }
    }
    Tarjeta("Detalles") {
        Interruptor("Sombra", activo = c.sombra) { v -> ConfigRepository.update { it.copy(sombra = v) } }
        Interruptor("Bordes", activo = c.bordes) { v -> ConfigRepository.update { it.copy(bordes = v) } }
        Interruptor("Burbuja al presionar", "Muestra la letra encima del dedo", c.popup) { v -> ConfigRepository.update { it.copy(popup = v) } }
    }
}

@Composable
private fun SeccionEscritura(c: KeyboardConfig) {
    Tarjeta("Sensación") {
        Interruptor("Vibración", activo = c.vibracion) { v -> ConfigRepository.update { it.copy(vibracion = v) } }
        if (c.vibracion) {
            Deslizador("Fuerza", c.fuerzaVibracion, 0.05f..1f, ::pct) { v -> ConfigRepository.update { it.copy(fuerzaVibracion = v) } }
        }
        Interruptor("Sonido", activo = c.sonido) { v -> ConfigRepository.update { it.copy(sonido = v) } }
        if (c.sonido) {
            Deslizador("Volumen", c.volumenSonido, 0.05f..1f, ::pct) { v -> ConfigRepository.update { it.copy(volumenSonido = v) } }
        }
    }
    Tarjeta("Ayudas") {
        Interruptor("Mayúscula automática", "Al iniciar cada oración", c.mayusAuto) { v -> ConfigRepository.update { it.copy(mayusAuto = v) } }
        Interruptor("Punto con doble espacio", activo = c.puntoDobleEspacio) { v -> ConfigRepository.update { it.copy(puntoDobleEspacio = v) } }
        Interruptor("Mover cursor deslizando el espacio", activo = c.cursorEnEspacio) { v -> ConfigRepository.update { it.copy(cursorEnEspacio = v) } }
        Interruptor("Acentos manteniendo la tecla", "Mantén «a» para á, à, ä…", c.acentosLargos) { v -> ConfigRepository.update { it.copy(acentosLargos = v) } }
    }
    OutlinedButton(onClick = { ConfigRepository.restablecer() }, modifier = Modifier.fillMaxWidth()) {
        Text("Restablecer todo")
    }
}
