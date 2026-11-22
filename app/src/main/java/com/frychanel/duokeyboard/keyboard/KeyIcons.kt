package com.frychanel.duokeyboard.keyboard

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.frychanel.duokeyboard.data.LayoutMode

/** Íconos dibujados a mano (estilo iOS), sin depender de librerías de íconos grandes. */

@Composable
fun ShiftIcon(estado: ShiftState, color: Color, modifier: Modifier) = Canvas(modifier) {
    val w = size.width
    val h = size.height
    val p = Path().apply {
        moveTo(w * 0.5f, h * 0.08f)
        lineTo(w * 0.95f, h * 0.55f)
        lineTo(w * 0.70f, h * 0.55f)
        lineTo(w * 0.70f, h * 0.84f)
        lineTo(w * 0.30f, h * 0.84f)
        lineTo(w * 0.30f, h * 0.55f)
        lineTo(w * 0.05f, h * 0.55f)
        close()
    }
    if (estado == ShiftState.OFF) {
        drawPath(p, color, style = Stroke(width = 1.7.dp.toPx(), join = StrokeJoin.Round))
    } else {
        drawPath(p, color)
    }
    if (estado == ShiftState.LOCK) {
        drawLine(color, Offset(w * 0.3f, h * 0.98f), Offset(w * 0.7f, h * 0.98f), 1.9.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
fun BackspaceIcon(color: Color, modifier: Modifier) = Canvas(modifier) {
    val w = size.width
    val h = size.height
    val trazo = Stroke(width = 1.7.dp.toPx(), join = StrokeJoin.Round, cap = StrokeCap.Round)
    val p = Path().apply {
        moveTo(w * 0.32f, h * 0.2f)
        lineTo(w * 0.95f, h * 0.2f)
        lineTo(w * 0.95f, h * 0.8f)
        lineTo(w * 0.32f, h * 0.8f)
        lineTo(w * 0.04f, h * 0.5f)
        close()
    }
    drawPath(p, color, style = trazo)
    drawLine(color, Offset(w * 0.5f, h * 0.37f), Offset(w * 0.76f, h * 0.63f), trazo.width, StrokeCap.Round)
    drawLine(color, Offset(w * 0.76f, h * 0.37f), Offset(w * 0.5f, h * 0.63f), trazo.width, StrokeCap.Round)
}

@Composable
fun ReturnIcon(color: Color, modifier: Modifier) = Canvas(modifier) {
    val w = size.width
    val h = size.height
    val s = 1.8.dp.toPx()
    val p = Path().apply {
        moveTo(w * 0.85f, h * 0.18f)
        lineTo(w * 0.85f, h * 0.62f)
        lineTo(w * 0.16f, h * 0.62f)
    }
    drawPath(p, color, style = Stroke(s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawLine(color, Offset(w * 0.16f, h * 0.62f), Offset(w * 0.36f, h * 0.42f), s, StrokeCap.Round)
    drawLine(color, Offset(w * 0.16f, h * 0.62f), Offset(w * 0.36f, h * 0.82f), s, StrokeCap.Round)
}

@Composable
fun GlobeIcon(color: Color, modifier: Modifier) = Canvas(modifier) {
    val s = 1.5.dp.toPx()
    val r = size.minDimension / 2f - s
    val c = center
    drawCircle(color, r, c, style = Stroke(s))
    drawOval(color, Offset(c.x - r * 0.45f, c.y - r), Size(r * 0.9f, r * 2f), style = Stroke(s))
    drawLine(color, Offset(c.x - r, c.y), Offset(c.x + r, c.y), s)
    drawLine(color, Offset(c.x - r * 0.85f, c.y - r * 0.5f), Offset(c.x + r * 0.85f, c.y - r * 0.5f), s)
    drawLine(color, Offset(c.x - r * 0.85f, c.y + r * 0.5f), Offset(c.x + r * 0.85f, c.y + r * 0.5f), s)
}

/** Ícono que muestra el modo actual: completo, duo o una mano. */
@Composable
fun LayoutIcon(modo: LayoutMode, color: Color, modifier: Modifier) = Canvas(modifier) {
    val w = size.width
    val h = size.height
    val s = 1.6.dp.toPx()
    val cr = CornerRadius(2.5.dp.toPx())
    val top = h * 0.22f
    val alto = h * 0.56f
    fun caja(x: Float, ancho: Float) =
        drawRoundRect(color, Offset(x, top), Size(ancho, alto), cr, style = Stroke(s))
    when (modo) {
        LayoutMode.COMPLETO -> caja(w * 0.04f, w * 0.92f)
        LayoutMode.DUO -> { caja(w * 0.02f, w * 0.40f); caja(w * 0.58f, w * 0.40f) }
        LayoutMode.MANO_IZQ -> caja(w * 0.02f, w * 0.62f)
        LayoutMode.MANO_DER -> caja(w * 0.36f, w * 0.62f)
    }
}

@Composable
fun PaletteIcon(color: Color, modifier: Modifier) = Canvas(modifier) {
    val s = 1.6.dp.toPx()
    val r = size.minDimension / 2f - s
    drawCircle(color, r, style = Stroke(s))
    drawArc(color, 90f, 180f, useCenter = true, topLeft = Offset(center.x - r, center.y - r), size = Size(r * 2, r * 2))
}

@Composable
fun ImageIcon(oculto: Boolean, color: Color, modifier: Modifier) = Canvas(modifier) {
    val w = size.width
    val h = size.height
    val s = 1.6.dp.toPx()
    drawRoundRect(color, Offset(w * 0.06f, h * 0.16f), Size(w * 0.88f, h * 0.68f), CornerRadius(3.dp.toPx()), style = Stroke(s))
    val monte = Path().apply {
        moveTo(w * 0.14f, h * 0.76f)
        lineTo(w * 0.40f, h * 0.46f)
        lineTo(w * 0.58f, h * 0.64f)
        lineTo(w * 0.70f, h * 0.54f)
        lineTo(w * 0.86f, h * 0.76f)
    }
    drawPath(monte, color, style = Stroke(s, join = StrokeJoin.Round, cap = StrokeCap.Round))
    drawCircle(color, w * 0.07f, Offset(w * 0.70f, h * 0.34f))
    if (oculto) drawLine(color, Offset(w * 0.08f, h * 0.06f), Offset(w * 0.92f, h * 0.94f), s * 1.2f, StrokeCap.Round)
}
