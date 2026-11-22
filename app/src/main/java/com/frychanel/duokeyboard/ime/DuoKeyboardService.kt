package com.frychanel.duokeyboard.ime

import android.content.Intent
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.frychanel.duokeyboard.data.BackgroundStore
import com.frychanel.duokeyboard.data.ConfigRepository
import com.frychanel.duokeyboard.keyboard.DuoKeyboardView
import com.frychanel.duokeyboard.keyboard.KeyboardController
import com.frychanel.duokeyboard.settings.SettingsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * El teclado en sí. Un InputMethodService no es una Activity, así que le damos
 * a mano el Lifecycle / SavedState que Jetpack Compose necesita para funcionar.
 */
class DuoKeyboardService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private val scope = MainScope()
    private val fondo = mutableStateOf<ImageBitmap?>(null)
    private lateinit var controlador: KeyboardController

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        ConfigRepository.init(this)
        controlador = KeyboardController(
            context = this,
            icProvider = { currentInputConnection },
            editorProvider = { currentInputEditorInfo },
            alCambiarTeclado = ::cambiarTeclado,
            alOcultar = { requestHideSelf(0) },
            alAbrirAjustes = ::abrirAjustes,
        )

        // Recarga la imagen de fondo cuando cambia el tipo o se elige una nueva.
        scope.launch {
            ConfigRepository.config
                .map { it.fondoTipo to it.fondoVersion }
                .distinctUntilChanged()
                .collect { (tipo, _) ->
                    fondo.value = withContext(Dispatchers.IO) { BackgroundStore.cargar(this@DuoKeyboardService, tipo) }
                }
        }
    }

    override fun onCreateInputView(): View {
        window?.window?.decorView?.let(::instalarPropietarios)
        return ComposeView(this).also { vista ->
            instalarPropietarios(vista)
            vista.setContent {
                val config by ConfigRepository.config.collectAsState()
                val horizontal = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                DuoKeyboardView(controlador, config, fondo.value, horizontal)
            }
        }
    }

    private fun instalarPropietarios(v: View) {
        v.setViewTreeLifecycleOwner(this)
        v.setViewTreeViewModelStoreOwner(this)
        v.setViewTreeSavedStateRegistryOwner(this)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        controlador.alIniciar(info)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        controlador.alTerminar()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        controlador.alCambiarSeleccion()
    }

    /** Nunca pantalla completa en horizontal: así se ve el modo Duo. */
    override fun onEvaluateFullscreenMode(): Boolean = false

    private fun cambiarTeclado() {
        val imm = getSystemService(InputMethodManager::class.java)
        val cambiado = if (Build.VERSION.SDK_INT >= 28) {
            switchToNextInputMethod(false)
        } else {
            @Suppress("DEPRECATION")
            imm.switchToNextInputMethod(window?.window?.attributes?.token, false)
        }
        if (!cambiado) imm.showInputMethodPicker()
    }

    private fun abrirAjustes() {
        startActivity(Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        requestHideSelf(0)
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        scope.cancel()
        store.clear()
        super.onDestroy()
    }
}
