package com.frychanel.duokeyboard.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Paleta para el tema personalizado. */
val PALETA: List<Long> = listOf(
    0xFFFFFFFF, 0xFFD1D3D9, 0xFF8E8E93, 0xFF3A3A3C, 0xFF1C1C1E, 0xFF000000,
    0xFF0A84FF, 0xFF5E5CE6, 0xFFBF5AF2, 0xFFFF2D55, 0xFFFF453A, 0xFFFF9F0A,
    0xFFFFD60A, 0xFF30D158, 0xFF66D4CF, 0xFF64D2FF, 0xFF0F3D2E, 0xFF6A3093,
)

@Composable
fun Tarjeta(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            titulo.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp,
        )
        contenido()
    }
}

@Composable
fun Deslizador(
    etiqueta: String,
    valor: Float,
    rango: ClosedFloatingPointRange<Float>,
    formato: (Float) -> String,
    alCambiar: (Float) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(etiqueta, Modifier.weight(1f))
            Text(formato(valor), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }
        Slider(value = valor.coerceIn(rango), onValueChange = alCambiar, valueRange = rango)
    }
}

@Composable
fun Interruptor(etiqueta: String, descripcion: String? = null, activo: Boolean, alCambiar: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { alCambiar(!activo) }.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(etiqueta)
            if (descripcion != null) {
                Text(descripcion, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = activo, onCheckedChange = alCambiar)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> Chips(opciones: List<T>, seleccionado: T, etiqueta: (T) -> String, alElegir: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        opciones.forEach { op ->
            FilterChip(selected = op == seleccionado, onClick = { alElegir(op) }, label = { Text(etiqueta(op)) })
        }
    }
}

@Composable
fun SelectorColor(etiqueta: String, actual: Long, alElegir: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(etiqueta)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PALETA.forEach { color ->
                val elegido = color == actual
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .border(
                            width = if (elegido) 3.dp else 1.dp,
                            color = if (elegido) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                            shape = CircleShape,
                        )
                        .clickable { alElegir(color) }
                )
            }
        }
    }
}
