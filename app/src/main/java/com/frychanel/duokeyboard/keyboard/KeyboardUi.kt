package com.frychanel.duokeyboard.keyboard

import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.frychanel.duokeyboard.data.BackgroundType
import com.frychanel.duokeyboard.data.ConfigRepository
import com.frychanel.duokeyboard.data.KeyStyle
import com.frychanel.duokeyboard.data.KeyboardConfig
import com.frychanel.duokeyboard.data.LayoutMode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

private const val LARGO_MS = 420L
private const val TICK_MS = 25L
private const val RETARDO_REPETICION = 380L
private const val INTERVALO_REPETICION = 70L

class CoordsRef { var value: LayoutCoordinates? = null }

private class Env(
    val controller: KeyboardController,
    val config: KeyboardConfig,
    val theme: KeyTheme,
    val rowHeight: Dp,
    val root: CoordsRef,
)

/**
 * El teclado completo. Se usa tanto dentro del servicio (teclado real)
 * como en la vista previa de la app de ajustes.
 */
@Composable
fun DuoKeyboardView(
    controller: KeyboardController,
    config: KeyboardConfig,
    fondo: ImageBitmap?,
    horizontal: Boolean,
    modifier: Modifier = Modifier,
    respetarBarraNavegacion: Boolean = true,
) {
    val theme = remember(config) { themeFor(config) }
    val root = remember { CoordsRef() }
    val rowHeight = (44f * config.alturaEscala * (if (horizontal) 0.78f else 1f)).dp
    val env = Env(controller, config, theme, rowHeight, root)

    Box(
        modifier
            .fillMaxWidth()
            .background(theme.fondo)
            .onGloballyPositioned { root.value = it }
    ) {
        if (config.fondoTipo != BackgroundType.TEMA && !config.fondoOculto && fondo != null) {
            Image(
                bitmap = fondo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .blur(config.fondoDesenfoque.dp), // Android 12+; en versiones viejas se ignora
            )
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = config.fondoOscurecer)))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (respetarBarraNavegacion) Modifier.navigationBarsPadding() else Modifier)
        ) {
            FranjaSuperior(env)
            AreaTeclas(env, horizontal)
            if (config.elevacion > 0f) Spacer(Modifier.height(config.elevacion.dp))
        }
        BurbujaTecla(env)
    }
}

// ------------------------------------------------------------------
// Barra superior: herramientas o acentos
// ------------------------------------------------------------------

@Composable
private fun FranjaSuperior(env: Env) {
    when {
        env.controller.acentos.isNotEmpty() -> BarraAcentos(env)
        env.config.barraHerramientas -> BarraHerramientas(env)
        else -> Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun BotonBarra(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(9.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun BarraHerramientas(env: Env) {
    val c = env.controller
    val cfg = env.config
    val color = env.theme.iconos
    val icono = Modifier.size(20.dp)
    Row(
        Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BotonBarra(c.alAbrirAjustes) { Icon(Icons.Filled.Settings, "Ajustes", icono, tint = color) }
        BotonBarra({ ConfigRepository.update { it.copy(modoTeclado = it.modoTeclado.siguiente()) } }) {
            LayoutIcon(cfg.modoTeclado, color, icono)
        }
        BotonBarra({ ConfigRepository.update { it.copy(tema = it.tema.siguiente()) } }) {
            PaletteIcon(color, icono)
        }
        if (cfg.fondoTipo != BackgroundType.TEMA) {
            BotonBarra({ ConfigRepository.update { it.copy(fondoOculto = !it.fondoOculto) } }) {
                ImageIcon(cfg.fondoOculto, color, icono)
            }
        }
        Spacer(Modifier.weight(1f))
        Text("duo", color = env.theme.acento, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
        Spacer(Modifier.weight(1f))
        if (!c.esVistaPrevia) {
            BotonBarra(c.alCambiarTeclado) { GlobeIcon(color, icono) }
        }
        BotonBarra(c.alOcultar) { Icon(Icons.Filled.KeyboardArrowDown, "Ocultar", Modifier.size(24.dp), tint = color) }
    }
}

@Composable
private fun BarraAcentos(env: Env) {
    val c = env.controller
    val th = env.theme
    Row(
        Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            c.acentos.forEach { s ->
                Box(
                    Modifier
                        .size(width = 42.dp, height = 34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(th.burbuja)
                        .clickable { c.elegirAcento(s) },
                    contentAlignment = Alignment.Center,
                ) { Text(s, color = th.textoBurbuja, fontSize = 20.sp) }
            }
        }
        BotonBarra(c::cerrarAcentos) { Icon(Icons.Filled.Close, "Cerrar", Modifier.size(20.dp), tint = th.iconos) }
    }
}

// ------------------------------------------------------------------
// Área de teclas: completo, duo y una mano
// ------------------------------------------------------------------

@Composable
private fun AreaTeclas(env: Env, horizontal: Boolean) {
    val cfg = env.config
    val filas = Layouts.filas(env.controller.page, cfg.filaNumeros)
    val modo = if (horizontal && cfg.duoEnHorizontal) LayoutMode.DUO else cfg.modoTeclado
    // Arco para modo completo: el centro baja hacia los pulgares.
    val arco: (Float) -> Float = { t -> cfg.curva * (1f - (2f * t - 1f) * (2f * t - 1f)) }

    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 3.dp).padding(bottom = 4.dp)) {
        val ancho = maxWidth
        when (modo) {
            LayoutMode.COMPLETO -> BloqueTeclas(
                env, filas.map { Layouts.unir(it) }, duo = false, curva = arco,
                modifier = Modifier.fillMaxWidth().padding(bottom = cfg.curva.dp),
            )

            LayoutMode.DUO -> {
                val hueco = ancho * cfg.separacionDuo
                val mitad = (ancho - hueco) / 2
                val rad = Math.toRadians(cfg.anguloDuo.toDouble())
                val extra = mitad * sin(rad).toFloat() + cfg.curva.dp
                Row(Modifier.fillMaxWidth().padding(bottom = extra)) {
                    // Mitad izquierda: gira desde la esquina inferior izquierda (pivote del pulgar)
                    BloqueTeclas(
                        env, filas.map { it.left }, duo = true,
                        curva = { t -> cfg.curva * t * t },
                        modifier = Modifier.width(mitad).graphicsLayer {
                            rotationZ = cfg.anguloDuo
                            transformOrigin = TransformOrigin(0f, 1f)
                        },
                    )
                    Spacer(Modifier.width(hueco))
                    BloqueTeclas(
                        env, filas.map { it.right }, duo = true,
                        curva = { t -> cfg.curva * (1f - t) * (1f - t) },
                        modifier = Modifier.width(mitad).graphicsLayer {
                            rotationZ = -cfg.anguloDuo
                            transformOrigin = TransformOrigin(1f, 1f)
                        },
                    )
                }
            }

            LayoutMode.MANO_IZQ, LayoutMode.MANO_DER -> {
                val anchoTeclado = ancho * cfg.anchoUnaMano
                val alto = env.rowHeight * filas.size
                Row(Modifier.fillMaxWidth()) {
                    if (modo == LayoutMode.MANO_DER) ControlesUnaMano(env, Modifier.weight(1f).height(alto), haciaIzquierda = true)
                    BloqueTeclas(
                        env, filas.map { Layouts.unir(it) }, duo = false, curva = arco,
                        modifier = Modifier.width(anchoTeclado).padding(bottom = cfg.curva.dp),
                    )
                    if (modo == LayoutMode.MANO_IZQ) ControlesUnaMano(env, Modifier.weight(1f).height(alto), haciaIzquierda = false)
                }
            }
        }
    }
}

@Composable
private fun ControlesUnaMano(env: Env, modifier: Modifier, haciaIzquierda: Boolean) {
    val color = env.theme.iconos
    Column(modifier, verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) {
        BotonBarra({
            ConfigRepository.update { it.copy(modoTeclado = if (haciaIzquierda) LayoutMode.MANO_IZQ else LayoutMode.MANO_DER) }
        }) {
            Icon(
                if (haciaIzquierda) Icons.Filled.KeyboardArrowLeft else Icons.Filled.KeyboardArrowRight,
                "Cambiar lado", Modifier.size(24.dp), tint = color,
            )
        }
        BotonBarra({ ConfigRepository.update { it.copy(modoTeclado = LayoutMode.DUO) } }) {
            LayoutIcon(LayoutMode.DUO, color, Modifier.size(20.dp))
        }
        BotonBarra({ ConfigRepository.update { it.copy(modoTeclado = LayoutMode.COMPLETO) } }) {
            LayoutIcon(LayoutMode.COMPLETO, color, Modifier.size(20.dp))
        }
    }
}

@Composable
private fun BloqueTeclas(env: Env, filas: List<List<Key>>, duo: Boolean, curva: (Float) -> Float, modifier: Modifier) {
    Column(modifier) {
        filas.forEach { fila -> FilaTeclas(env, fila, duo, curva) }
    }
}

private fun pesoDe(key: Key, duo: Boolean) = if (duo) key.duoWeight else key.weight

@Composable
private fun FilaTeclas(env: Env, teclas: List<Key>, duo: Boolean, curva: (Float) -> Float) {
    val total = teclas.sumOf { pesoDe(it, duo).toDouble() }.toFloat()
    Row(Modifier.fillMaxWidth().height(env.rowHeight)) {
        var acumulado = 0f
        teclas.forEach { key ->
            val peso = pesoDe(key, duo)
            val t = (acumulado + peso / 2f) / total
            acumulado += peso
            val desplazamiento = curva(t)
            Tecla(
                env, key,
                Modifier
                    .weight(peso)
                    .fillMaxHeight()
                    .graphicsLayer { translationY = desplazamiento.dp.toPx() },
            )
        }
    }
}

// ------------------------------------------------------------------
// Una tecla
// ------------------------------------------------------------------

@Composable
private fun Tecla(env: Env, key: Key, modifier: Modifier) {
    val c = env.controller
    val cfg = env.config
    val th = env.theme
    var presionada by remember { mutableStateOf(false) }
    val coords = remember { CoordsRef() }
    val root = env.root

    val esEspecial = key.type != KeyType.CHAR && key.type != KeyType.SPACE
    val enterAccion = key.type == KeyType.ENTER && c.enter.conAccion
    val shiftActivo = key.type == KeyType.SHIFT && c.shift != ShiftState.OFF
    val colorBase = when {
        enterAccion -> th.acento
        shiftActivo -> th.activa
        esEspecial -> th.especial
        else -> th.tecla
    }
    val colorTexto = when {
        enterAccion -> th.textoAcento
        shiftActivo -> th.textoActiva
        esEspecial -> th.textoEspecial
        else -> th.texto
    }
    val relleno = if (presionada) th.presionada else colorBase
    val estilo = cfg.estiloTecla
    val opacidad = cfg.opacidadTecla
    val conSombra = cfg.sombra && estilo == KeyStyle.SOLIDA
    val conBorde = cfg.bordes || estilo == KeyStyle.CONTORNO
    val radio = cfg.radioTecla.dp
    val density = LocalDensity.current
    val slop = with(density) { 10.dp.toPx() }
    val paso = with(density) { 9.dp.toPx() }

    Box(
        modifier
            .onGloballyPositioned { coords.value = it }
            .pointerInput(key) {
                gestosTecla(
                    repetible = key.type == KeyType.DELETE,
                    arrastrable = { key.type == KeyType.SPACE && ConfigRepository.config.value.cursorEnEspacio },
                    slop = slop,
                    paso = paso,
                    alBajar = {
                        presionada = true
                        c.alPresionar(key)
                        if (key.type == KeyType.CHAR && ConfigRepository.config.value.popup) {
                            val r = root.value
                            val k = coords.value
                            if (r != null && k != null && r.isAttached && k.isAttached) {
                                c.popup = PopupInfo(key, c.textoTecla(key), r.localBoundingBoxOf(k, false))
                            }
                        }
                    },
                    alTocar = { c.alTocar(key) },
                    alMantener = {
                        val hizoAlgo = c.alMantener(key)
                        if (hizoAlgo && c.popup?.key == key) c.popup = null
                        hizoAlgo
                    },
                    alRepetir = { c.alRepetir(key) },
                    alMoverCursor = { c.moverCursor(it) },
                    alSoltar = {
                        presionada = false
                        if (c.popup?.key == key) c.popup = null
                    },
                )
            }
            .padding(horizontal = (cfg.espaciado / 2f).dp, vertical = (cfg.espaciado / 2f + 1f).dp)
            .drawBehind {
                val cr = CornerRadius(radio.toPx())
                val colorRelleno = when (estilo) {
                    KeyStyle.SOLIDA -> relleno.copy(alpha = relleno.alpha * opacidad)
                    KeyStyle.CONTORNO -> relleno.copy(alpha = relleno.alpha * opacidad * (if (presionada || enterAccion || shiftActivo) 0.95f else 0.3f))
                    KeyStyle.MINIMAL -> if (presionada || enterAccion || shiftActivo) relleno.copy(alpha = relleno.alpha * opacidad) else Color.Transparent
                }
                if (conSombra) {
                    drawRoundRect(th.sombra.copy(alpha = th.sombra.alpha * opacidad), Offset(0f, 1.2.dp.toPx()), size, cr)
                }
                drawRoundRect(colorRelleno, cornerRadius = cr)
                if (conBorde) {
                    val colorBorde = if (estilo == KeyStyle.CONTORNO) colorTexto.copy(alpha = 0.45f) else th.borde
                    drawRoundRect(colorBorde, cornerRadius = cr, style = Stroke(1.dp.toPx()))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        ContenidoTecla(env, key, colorTexto)
    }
}

@Composable
private fun ContenidoTecla(env: Env, key: Key, color: Color) {
    val c = env.controller
    val fs = env.rowHeight.value * 0.5f * env.config.escalaTexto
    val icono = env.rowHeight * 0.44f * env.config.escalaTexto
    when (key.type) {
        KeyType.SHIFT -> ShiftIcon(c.shift, color, Modifier.size(icono))
        KeyType.DELETE -> BackspaceIcon(color, Modifier.size(icono * 1.15f))
        KeyType.ENTER -> {
            val etiqueta = c.enter.etiqueta
            if (etiqueta != null) {
                Text(etiqueta, color = color, fontSize = (fs * 0.6f).sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
            } else {
                ReturnIcon(color, Modifier.size(icono))
            }
        }
        KeyType.SPACE -> Text("espacio", color = color.copy(alpha = 0.75f), fontSize = (fs * 0.56f).sp, maxLines = 1, softWrap = false)
        KeyType.CHAR -> {
            val texto = c.textoTecla(key)
            val tam = if (texto.firstOrNull()?.isLetter() == true) fs else fs * 0.85f
            Text(texto, color = color, fontSize = tam.sp, maxLines = 1, softWrap = false)
        }
        else -> Text(key.label, color = color, fontSize = (fs * 0.6f).sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
    }
}

/** Burbuja que aparece encima de la tecla al presionarla (como en iOS). */
@Composable
private fun BurbujaTecla(env: Env) {
    val p = env.controller.popup ?: return
    val density = LocalDensity.current
    val th = env.theme
    val anchoPx = max(p.bounds.width * 1.2f, with(density) { 46.dp.toPx() })
    val altoPx = with(density) { (env.rowHeight * 1.1f).toPx() }
    val margen = with(density) { 4.dp.toPx() }
    val x = (p.bounds.center.x - anchoPx / 2f).coerceAtLeast(0f)
    val y = (p.bounds.top - altoPx - margen).coerceAtLeast(0f)
    val forma = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .size(with(density) { anchoPx.toDp() }, with(density) { altoPx.toDp() })
            .shadow(8.dp, forma)
            .background(th.burbuja, forma),
        contentAlignment = Alignment.Center,
    ) {
        Text(p.texto, color = th.textoBurbuja, fontSize = (env.rowHeight.value * 0.68f).sp)
    }
}

// ------------------------------------------------------------------
// Gestos: tocar, mantener, repetir (borrar) y deslizar (cursor en espacio)
// ------------------------------------------------------------------

private suspend fun PointerInputScope.gestosTecla(
    repetible: Boolean,
    arrastrable: () -> Boolean,
    slop: Float,
    paso: Float,
    alBajar: () -> Unit,
    alTocar: () -> Unit,
    alMantener: () -> Boolean,
    alRepetir: () -> Unit,
    alMoverCursor: (Int) -> Unit,
    alSoltar: () -> Unit,
) = awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    down.consume()
    alBajar()
    val inicio = SystemClock.uptimeMillis()
    var largoRevisado = false
    var mantenido = false
    var modoCursor = false
    var acumulado = 0f
    var ultimoX = down.position.x
    var siguienteRepeticion = inicio + RETARDO_REPETICION
    var intervalo = INTERVALO_REPETICION
    try {
        while (true) {
            val evento = withTimeoutOrNull(TICK_MS) { awaitPointerEvent() }
            val ahora = SystemClock.uptimeMillis()
            if (repetible) {
                if (ahora >= siguienteRepeticion) {
                    alRepetir()
                    intervalo = (intervalo - 5L).coerceAtLeast(30L) // acelera mientras mantienes
                    siguienteRepeticion = ahora + intervalo
                }
            } else if (!largoRevisado && !modoCursor && ahora - inicio >= LARGO_MS) {
                largoRevisado = true
                mantenido = alMantener()
            }
            if (evento == null) continue

            val cambio = evento.changes.firstOrNull { it.id == down.id } ?: break
            if (!cambio.pressed) {
                cambio.consume()
                if (!mantenido && !modoCursor) alTocar()
                break
            }
            if (arrastrable()) {
                val total = cambio.position.x - down.position.x
                if (!modoCursor && abs(total) > slop) {
                    modoCursor = true
                    acumulado = total
                } else if (modoCursor) {
                    acumulado += cambio.position.x - ultimoX
                }
                if (modoCursor) {
                    val pasos = (acumulado / paso).toInt()
                    if (pasos != 0) {
                        alMoverCursor(pasos)
                        acumulado -= pasos * paso
                    }
                }
            }
            ultimoX = cambio.position.x
            cambio.consume()
        }
    } finally {
        alSoltar()
    }
}
