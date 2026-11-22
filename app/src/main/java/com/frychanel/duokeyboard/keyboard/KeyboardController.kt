package com.frychanel.duokeyboard.keyboard

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.InputType
import android.text.TextUtils
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import com.frychanel.duokeyboard.data.ConfigRepository
import kotlin.math.abs

enum class Page { LETRAS, SIMBOLOS, SIMBOLOS2 }
enum class ShiftState { OFF, ONCE, LOCK }

data class PopupInfo(val key: Key, val texto: String, val bounds: Rect)
data class EnterInfo(val etiqueta: String?, val conAccion: Boolean)

/**
 * Estado del teclado + todo lo que pasa al tocar una tecla.
 * Si [icProvider] devuelve null (vista previa en ajustes) solo cambia el estado visual.
 */
class KeyboardController(
    context: Context,
    private val icProvider: () -> InputConnection?,
    private val editorProvider: () -> EditorInfo?,
    val alCambiarTeclado: () -> Unit = {},
    val alOcultar: () -> Unit = {},
    val alAbrirAjustes: () -> Unit = {},
    val esVistaPrevia: Boolean = false,
) {
    var page by mutableStateOf(Page.LETRAS)
        private set
    var shift by mutableStateOf(ShiftState.OFF)
        private set
    var enter by mutableStateOf(EnterInfo(null, false))
        private set
    var acentos by mutableStateOf<List<String>>(emptyList())
        private set
    var popup by mutableStateOf<PopupInfo?>(null)

    private val feedback = Feedback(context.applicationContext)
    private var ultimoShift = 0L
    private var ultimoEspacio = 0L
    private val cfg get() = ConfigRepository.config.value

    fun textoTecla(key: Key): String =
        if (key.type == KeyType.CHAR && page == Page.LETRAS && shift != ShiftState.OFF) key.label.uppercase()
        else key.label

    // ---------- Ciclo de vida ----------

    fun alIniciar(info: EditorInfo?) {
        val clase = (info?.inputType ?: 0) and InputType.TYPE_MASK_CLASS
        page = when (clase) {
            InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_PHONE, InputType.TYPE_CLASS_DATETIME -> Page.SIMBOLOS
            else -> Page.LETRAS
        }
        shift = ShiftState.OFF
        acentos = emptyList()
        popup = null
        enter = calcularEnter(info)
        actualizarMayusAuto()
    }

    fun alTerminar() {
        acentos = emptyList()
        popup = null
    }

    fun alCambiarSeleccion() {
        if (acentos.isEmpty()) actualizarMayusAuto()
    }

    // ---------- Eventos de teclas ----------

    fun alPresionar(key: Key) {
        feedback.reproducir(key.type)
        if (key.type == KeyType.DELETE) borrar()
    }

    fun alTocar(key: Key) {
        when (key.type) {
            KeyType.CHAR -> escribir(textoTecla(key))
            KeyType.SPACE -> espacio()
            KeyType.ENTER -> enterPulsado()
            KeyType.SHIFT -> alternarShift()
            KeyType.TO_SYMBOLS -> { page = Page.SIMBOLOS; acentos = emptyList() }
            KeyType.TO_LETTERS -> { page = Page.LETRAS; actualizarMayusAuto() }
            KeyType.TO_SYMBOLS2 -> page = if (page == Page.SIMBOLOS2) Page.SIMBOLOS else Page.SIMBOLOS2
            KeyType.DELETE -> Unit // ya se borró al presionar
        }
    }

    /** @return true si la pulsación larga hizo algo (y entonces se ignora el toque). */
    fun alMantener(key: Key): Boolean {
        if (key.type == KeyType.CHAR && cfg.acentosLargos && key.alts.isNotEmpty()) {
            val mayus = page == Page.LETRAS && shift != ShiftState.OFF
            acentos = (listOf(key.label) + key.alts).map { if (mayus) it.uppercase() else it }
            feedback.reproducir(KeyType.SHIFT)
            return true
        }
        return false
    }

    fun alRepetir(key: Key) {
        if (key.type == KeyType.DELETE) borrar()
    }

    fun moverCursor(pasos: Int) {
        val code = if (pasos > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        repeat(abs(pasos)) { enviarTecla(code) }
    }

    fun elegirAcento(s: String) {
        escribir(s)
        acentos = emptyList()
    }

    fun cerrarAcentos() {
        acentos = emptyList()
    }

    // ---------- Lógica de texto ----------

    private fun escribir(texto: String) {
        icProvider()?.commitText(texto, 1)
        if (shift == ShiftState.ONCE) shift = ShiftState.OFF
        acentos = emptyList()
        actualizarMayusAuto()
    }

    private fun espacio() {
        val ic = icProvider()
        val ahora = SystemClock.uptimeMillis()
        if (ic != null && cfg.puntoDobleEspacio && ahora - ultimoEspacio < 700) {
            val antes = ic.getTextBeforeCursor(2, 0)
            if (antes != null && antes.length == 2 && antes[1] == ' ' && antes[0].isLetterOrDigit()) {
                ic.deleteSurroundingText(1, 0)
                ic.commitText(". ", 1)
                ultimoEspacio = 0
                actualizarMayusAuto()
                return
            }
        }
        ic?.commitText(" ", 1)
        ultimoEspacio = ahora
        if (page != Page.LETRAS) page = Page.LETRAS
        if (shift == ShiftState.ONCE) shift = ShiftState.OFF
        actualizarMayusAuto()
    }

    private fun borrar() {
        val ic = icProvider() ?: return
        val seleccion = ic.getSelectedText(0)
        when {
            !seleccion.isNullOrEmpty() -> ic.commitText("", 1)
            ic.getTextBeforeCursor(1, 0).isNullOrEmpty() -> enviarTecla(KeyEvent.KEYCODE_DEL)
            else -> ic.deleteSurroundingTextInCodePoints(1, 0)
        }
        actualizarMayusAuto()
    }

    private fun enterPulsado() {
        val ic = icProvider() ?: return
        val info = editorProvider()
        if (enter.conAccion && info != null) {
            ic.performEditorAction(info.imeOptions and EditorInfo.IME_MASK_ACTION)
        } else {
            enviarTecla(KeyEvent.KEYCODE_ENTER)
        }
        actualizarMayusAuto()
    }

    private fun alternarShift() {
        val ahora = SystemClock.uptimeMillis()
        shift = when {
            shift == ShiftState.LOCK -> ShiftState.OFF
            ahora - ultimoShift < 350 -> ShiftState.LOCK   // doble toque = bloq mayús
            shift == ShiftState.OFF -> ShiftState.ONCE
            else -> ShiftState.OFF
        }
        ultimoShift = ahora
    }

    private fun actualizarMayusAuto() {
        if (!cfg.mayusAuto || shift == ShiftState.LOCK || page != Page.LETRAS) return
        val info = editorProvider() ?: return
        val ic = icProvider() ?: return
        if ((info.inputType and InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT) return
        val variacion = info.inputType and InputType.TYPE_MASK_VARIATION
        val sinMayus = setOf(
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_URI,
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
        )
        if (variacion in sinMayus) return
        val caps = ic.getCursorCapsMode(TextUtils.CAP_MODE_SENTENCES) != 0
        shift = if (caps) ShiftState.ONCE else ShiftState.OFF
    }

    private fun calcularEnter(info: EditorInfo?): EnterInfo {
        info ?: return EnterInfo(null, false)
        if ((info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) return EnterInfo(null, false)
        return when (info.imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_GO -> EnterInfo("ir", true)
            EditorInfo.IME_ACTION_SEARCH -> EnterInfo("buscar", true)
            EditorInfo.IME_ACTION_SEND -> EnterInfo("enviar", true)
            EditorInfo.IME_ACTION_NEXT -> EnterInfo("sig.", true)
            EditorInfo.IME_ACTION_DONE -> EnterInfo("listo", true)
            EditorInfo.IME_ACTION_PREVIOUS -> EnterInfo("ant.", true)
            else -> EnterInfo(null, false)
        }
    }

    private fun enviarTecla(code: Int) {
        val ic = icProvider() ?: return
        val t = SystemClock.uptimeMillis()
        ic.sendKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, code, 0))
        ic.sendKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_UP, code, 0))
    }
}

/** Vibración y sonido al teclear, con la fuerza/volumen elegidos. */
private class Feedback(ctx: Context) {
    private val vibrador: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        ctx.getSystemService(Vibrator::class.java)
    }
    private val audio: AudioManager? = ctx.getSystemService(AudioManager::class.java)

    fun reproducir(tipo: KeyType) {
        val c = ConfigRepository.config.value
        if (c.vibracion) vibrar(c.fuerzaVibracion)
        if (c.sonido) {
            val efecto = when (tipo) {
                KeyType.DELETE -> AudioManager.FX_KEYPRESS_DELETE
                KeyType.ENTER -> AudioManager.FX_KEYPRESS_RETURN
                KeyType.SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
                else -> AudioManager.FX_KEYPRESS_STANDARD
            }
            audio?.playSoundEffect(efecto, c.volumenSonido)
        }
    }

    private fun vibrar(fuerza: Float) {
        val v = vibrador ?: return
        if (!v.hasVibrator()) return
        val ms = (6 + fuerza * 22).toLong()
        val amp = if (v.hasAmplitudeControl()) (fuerza * 254 + 1).toInt().coerceIn(1, 255) else VibrationEffect.DEFAULT_AMPLITUDE
        v.vibrate(VibrationEffect.createOneShot(ms, amp))
    }
}
