package com.frychanel.duokeyboard.keyboard

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.frychanel.duokeyboard.data.KeyboardConfig
import com.frychanel.duokeyboard.data.ThemeId

data class KeyTheme(
    val fondo: Brush,
    val tecla: Color,
    val especial: Color,
    val activa: Color,
    val texto: Color,
    val textoEspecial: Color,
    val textoActiva: Color,
    val acento: Color,
    val presionada: Color,
    val borde: Color,
    val sombra: Color,
    val iconos: Color,
    val burbuja: Color,
    val textoBurbuja: Color,
) {
    /** Texto legible sobre el color de acento (tecla Enter con acción). */
    val textoAcento: Color get() = if (acento.luminance() > 0.55f) Color.Black else Color.White
}

private fun solido(c: Long): Brush = SolidColor(Color(c))
private fun degradado(a: Long, b: Long): Brush = Brush.verticalGradient(listOf(Color(a), Color(b)))

fun themeFor(c: KeyboardConfig, id: ThemeId = c.tema): KeyTheme = when (id) {
    ThemeId.IOS_CLARO -> KeyTheme(
        fondo = solido(0xFFD1D3D9), tecla = Color.White, especial = Color(0xFFABB0BC),
        activa = Color.White, texto = Color.Black, textoEspecial = Color.Black, textoActiva = Color.Black,
        acento = Color(0xFF007AFF), presionada = Color(0xFFB8BCC6), borde = Color(0x1A000000),
        sombra = Color(0x59898A8D), iconos = Color(0xFF3C3C43), burbuja = Color.White, textoBurbuja = Color.Black,
    )
    ThemeId.IOS_OSCURO -> KeyTheme(
        fondo = solido(0xFF202022), tecla = Color(0xFF56565A), especial = Color(0xFF38383B),
        activa = Color(0xFFE5E5EA), texto = Color.White, textoEspecial = Color.White, textoActiva = Color.Black,
        acento = Color(0xFF0A84FF), presionada = Color(0xFF77777C), borde = Color(0x26FFFFFF),
        sombra = Color(0xFF0E0E0F), iconos = Color(0xFFD1D1D6), burbuja = Color(0xFF6C6C70), textoBurbuja = Color.White,
    )
    ThemeId.CRISTAL -> KeyTheme(
        fondo = degradado(0xFF0F172A, 0xFF1E3A8A), tecla = Color(0x2EFFFFFF), especial = Color(0x17FFFFFF),
        activa = Color(0xE6FFFFFF), texto = Color.White, textoEspecial = Color.White, textoActiva = Color(0xFF0F172A),
        acento = Color(0xFF38BDF8), presionada = Color(0x59FFFFFF), borde = Color(0x40FFFFFF),
        sombra = Color(0x33000000), iconos = Color(0xFFE0F2FE), burbuja = Color(0xFF334E8C), textoBurbuja = Color.White,
    )
    ThemeId.NEON -> KeyTheme(
        fondo = degradado(0xFF07070F, 0xFF140A26), tecla = Color(0xFF15152A), especial = Color(0xFF0E0E1E),
        activa = Color(0xFF00F5FF), texto = Color(0xFF00F5FF), textoEspecial = Color(0xFFFF2BD6), textoActiva = Color(0xFF07070F),
        acento = Color(0xFFFF2BD6), presionada = Color(0xFF2A2A55), borde = Color(0x8000F5FF),
        sombra = Color(0x66FF2BD6), iconos = Color(0xFF00F5FF), burbuja = Color(0xFF1D1D3A), textoBurbuja = Color(0xFF00F5FF),
    )
    ThemeId.AMOLED -> KeyTheme(
        fondo = solido(0xFF000000), tecla = Color(0xFF171717), especial = Color(0xFF0D0D0D),
        activa = Color(0xFFEDEDED), texto = Color(0xFFEDEDED), textoEspecial = Color(0xFFBDBDBD), textoActiva = Color.Black,
        acento = Color(0xFF30D158), presionada = Color(0xFF2E2E2E), borde = Color(0x1FFFFFFF),
        sombra = Color.Transparent, iconos = Color(0xFFBDBDBD), burbuja = Color(0xFF262626), textoBurbuja = Color.White,
    )
    ThemeId.ATARDECER -> KeyTheme(
        fondo = degradado(0xFFFF7E5F, 0xFF6A3093), tecla = Color(0x40FFFFFF), especial = Color(0x24FFFFFF),
        activa = Color(0xF2FFFFFF), texto = Color.White, textoEspecial = Color.White, textoActiva = Color(0xFF6A3093),
        acento = Color(0xFFFFD166), presionada = Color(0x73FFFFFF), borde = Color(0x4DFFFFFF),
        sombra = Color(0x26000000), iconos = Color.White, burbuja = Color(0xFFB4507A), textoBurbuja = Color.White,
    )
    ThemeId.MENTA -> KeyTheme(
        fondo = degradado(0xFFE6F7EF, 0xFFCDEFE0), tecla = Color.White, especial = Color(0xFFB5E3CE),
        activa = Color(0xFF0F3D2E), texto = Color(0xFF0F3D2E), textoEspecial = Color(0xFF0F3D2E), textoActiva = Color.White,
        acento = Color(0xFF10B981), presionada = Color(0xFFC9EBDB), borde = Color(0x1A0F3D2E),
        sombra = Color(0x400F3D2E), iconos = Color(0xFF0F3D2E), burbuja = Color.White, textoBurbuja = Color(0xFF0F3D2E),
    )
    ThemeId.PERSONAL -> {
        val fondo = Color(c.colorFondo)
        val tecla = Color(c.colorTecla)
        val texto = Color(c.colorTexto)
        val activa = lerp(tecla, texto, 0.85f)
        KeyTheme(
            fondo = SolidColor(fondo), tecla = tecla, especial = lerp(tecla, fondo, 0.5f),
            activa = activa, texto = texto, textoEspecial = texto,
            textoActiva = if (activa.luminance() > 0.5f) Color.Black else Color.White,
            acento = Color(c.colorAcento), presionada = lerp(tecla, texto, 0.25f),
            borde = texto.copy(alpha = 0.18f), sombra = Color.Black.copy(alpha = 0.3f),
            iconos = texto.copy(alpha = 0.85f), burbuja = lerp(tecla, texto, 0.12f).copy(alpha = 1f), textoBurbuja = texto,
        )
    }
}
