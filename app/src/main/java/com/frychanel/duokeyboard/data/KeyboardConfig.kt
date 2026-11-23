package com.frychanel.duokeyboard.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class LayoutMode(val etiqueta: String) {
    COMPLETO("Completo"),
    DUO("Duo · dividido"),
    MANO_IZQ("Una mano · izq"),
    MANO_DER("Una mano · der");

    fun siguiente(): LayoutMode = entries[(ordinal + 1) % entries.size]
}

@Serializable
enum class ThemeId(val etiqueta: String) {
    IOS_OSCURO("iOS Oscuro"),
    IOS_CLARO("iOS Claro"),
    CRISTAL("Cristal"),
    NEON("Neón"),
    AMOLED("AMOLED"),
    ATARDECER("Atardecer"),
    MENTA("Menta"),
    PERSONAL("Personalizado");

    fun siguiente(): ThemeId = entries[(ordinal + 1) % entries.size]
}

@Serializable
enum class KeyStyle(val etiqueta: String) {
    SOLIDA("Sólida"),
    CRISTAL("Cristal líquido"),
    CONTORNO("Contorno"),
    MINIMAL("Minimal")
}

@Serializable
enum class BackgroundType(val etiqueta: String) {
    TEMA("Color del tema"),
    GALERIA("Foto de galería"),
    SATELITE("Imagen satelital")
}

/** Todo lo que el usuario puede personalizar. Se guarda como JSON. */
@Serializable
data class KeyboardConfig(
    // ---- Diseño ----
    val modoTeclado: LayoutMode = LayoutMode.DUO,
    val duoEnHorizontal: Boolean = true,
    val alturaEscala: Float = 1f,
    val separacionDuo: Float = 0.14f,
    val anguloDuo: Float = 4f,
    val curva: Float = 0f,
    val anchoUnaMano: Float = 0.82f,
    val elevacion: Float = 0f,
    val filaNumeros: Boolean = false,
    val barraHerramientas: Boolean = true,

    // ---- Estilo ----
    val tema: ThemeId = ThemeId.IOS_OSCURO,
    val estiloTecla: KeyStyle = KeyStyle.SOLIDA,
    val colorFondo: Long = 0xFF1C1C1E,
    val colorTecla: Long = 0xFF3A3A3C,
    val colorTexto: Long = 0xFFFFFFFF,
    val colorAcento: Long = 0xFF0A84FF,

    // ---- Teclas ----
    val radioTecla: Float = 9f,
    val espaciado: Float = 6f,
    val opacidadTecla: Float = 1f,
    val escalaTexto: Float = 1f,
    val sombra: Boolean = true,
    val bordes: Boolean = false,
    val popup: Boolean = true,

    // ---- Fondo ----
    val fondoTipo: BackgroundType = BackgroundType.TEMA,
    val fondoOculto: Boolean = false,
    val fondoOscurecer: Float = 0.25f,
    val fondoDesenfoque: Float = 0f,
    val fondoVersion: Long = 0,
    val sateliteNombre: String = "",

    // ---- Escritura ----
    val vibracion: Boolean = true,
    val fuerzaVibracion: Float = 0.45f,
    val sonido: Boolean = false,
    val volumenSonido: Float = 0.5f,
    val mayusAuto: Boolean = true,
    val puntoDobleEspacio: Boolean = true,
    val cursorEnEspacio: Boolean = true,
    val acentosLargos: Boolean = true,
)

/**
 * Fuente única de la configuración. El teclado y la app de ajustes viven en el
 * mismo proceso, así que cualquier cambio se ve en vivo en ambos.
 */
object ConfigRepository {
    private const val PREFS = "duo_config"
    private const val KEY = "config_json"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private var prefs: SharedPreferences? = null
    private val _config = MutableStateFlow(KeyboardConfig())
    val config: StateFlow<KeyboardConfig> = _config.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        p.getString(KEY, null)?.let { raw ->
            runCatching { json.decodeFromString(KeyboardConfig.serializer(), raw) }
                .onSuccess { _config.value = it }
        }
    }

    fun update(transform: (KeyboardConfig) -> KeyboardConfig) {
        val nuevo = transform(_config.value)
        if (nuevo == _config.value) return
        _config.value = nuevo
        prefs?.edit()?.putString(KEY, json.encodeToString(KeyboardConfig.serializer(), nuevo))?.apply()
    }

    fun restablecer() = update { actual -> KeyboardConfig(fondoVersion = actual.fondoVersion + 1) }
}

// ---------------- Premium ----------------

private val TEMAS_GRATIS = setOf(ThemeId.IOS_OSCURO, ThemeId.IOS_CLARO)

fun ThemeId.esPremium() = this !in TEMAS_GRATIS
fun KeyStyle.esPremium() = this == KeyStyle.CONTORNO || this == KeyStyle.MINIMAL
fun BackgroundType.esPremium() = this != BackgroundType.TEMA

/** Siguiente tema para el botón de la barra (sin Premium solo recorre los gratis). */
fun ThemeId.siguienteDisponible(premium: Boolean): ThemeId {
    var t = siguiente()
    while (!premium && t.esPremium()) t = t.siguiente()
    return t
}

/** Lo que el teclado realmente usa: sin Premium, las opciones de pago vuelven a las gratis. */
fun KeyboardConfig.efectiva(premium: Boolean): KeyboardConfig = if (premium) this else copy(
    tema = if (tema.esPremium()) ThemeId.IOS_OSCURO else tema,
    estiloTecla = if (estiloTecla.esPremium()) KeyStyle.SOLIDA else estiloTecla,
    fondoTipo = if (fondoTipo.esPremium()) BackgroundType.TEMA else fondoTipo,
)
