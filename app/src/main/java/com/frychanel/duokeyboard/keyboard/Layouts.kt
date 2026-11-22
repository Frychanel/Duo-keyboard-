package com.frychanel.duokeyboard.keyboard

enum class KeyType { CHAR, SHIFT, DELETE, ENTER, SPACE, TO_SYMBOLS, TO_LETTERS, TO_SYMBOLS2 }

/**
 * @param weight     ancho relativo en modo completo / una mano
 * @param duoWeight  ancho relativo dentro de su mitad en modo Duo
 */
data class Key(
    val type: KeyType,
    val label: String = "",
    val output: String = label,
    val weight: Float = 1f,
    val duoWeight: Float = weight,
    val alts: List<String> = emptyList(),
)

/** Cada fila ya viene partida en mitad izquierda y derecha (para el modo Duo). */
data class SplitRow(val left: List<Key>, val right: List<Key>)

object Layouts {

    private val ALTERNATIVAS = mapOf(
        "a" to listOf("á", "à", "ä", "â", "ã", "å", "æ"),
        "e" to listOf("é", "è", "ë", "ê", "€"),
        "i" to listOf("í", "ì", "ï", "î"),
        "o" to listOf("ó", "ò", "ö", "ô", "õ", "ø", "º"),
        "u" to listOf("ú", "ù", "ü", "û"),
        "n" to listOf("ñ"),
        "c" to listOf("ç", "©"),
        "s" to listOf("ß", "$"),
        "y" to listOf("ý", "ÿ"),
        "l" to listOf("ł"),
        "z" to listOf("ž"),
        "." to listOf("…", "·"),
        "," to listOf(";", ":"),
        "?" to listOf("¿"),
        "!" to listOf("¡"),
        "-" to listOf("–", "—", "_"),
        "$" to listOf("€", "£", "¥", "¢", "₿"),
        "'" to listOf("‘", "’", "`"),
        "\"" to listOf("“", "”", "«", "»"),
        "0" to listOf("°"),
        "1" to listOf("¹", "½"),
        "2" to listOf("²"),
        "3" to listOf("³"),
    )

    private fun c(s: String) = Key(KeyType.CHAR, s, alts = ALTERNATIVAS[s] ?: emptyList())
    private fun chars(vararg s: String) = s.map(::c)

    private val borrar = Key(KeyType.DELETE, weight = 1.5f, duoWeight = 2f)
    private val espacio = Key(KeyType.SPACE, "espacio", " ", weight = 2.7f)

    private fun filaInferior(modo: Key) = SplitRow(
        listOf(modo, c(","), espacio),
        listOf(espacio, c("."), Key(KeyType.ENTER, weight = 1.3f)),
    )

    private val filaNumeros = SplitRow(chars("1", "2", "3", "4", "5"), chars("6", "7", "8", "9", "0"))

    private val letras = listOf(
        SplitRow(chars("q", "w", "e", "r", "t"), chars("y", "u", "i", "o", "p")),
        SplitRow(chars("a", "s", "d", "f", "g"), chars("h", "j", "k", "l", "ñ")),
        SplitRow(
            listOf(Key(KeyType.SHIFT, weight = 1.5f, duoWeight = 1f)) + chars("z", "x", "c", "v"),
            chars("b", "n", "m") + borrar,
        ),
        filaInferior(Key(KeyType.TO_SYMBOLS, "123", weight = 1.3f)),
    )

    private val simbolos = listOf(
        filaNumeros,
        SplitRow(chars("-", "/", ":", ";", "("), chars(")", "$", "&", "@", "\"")),
        SplitRow(
            listOf(Key(KeyType.TO_SYMBOLS2, "#+=", weight = 1.5f, duoWeight = 1f)) + chars(".", ",", "?", "!"),
            chars("'", "*", "_") + borrar,
        ),
        filaInferior(Key(KeyType.TO_LETTERS, "ABC", weight = 1.3f)),
    )

    private val simbolos2 = listOf(
        SplitRow(chars("[", "]", "{", "}", "#"), chars("%", "^", "*", "+", "=")),
        SplitRow(chars("_", "\\", "|", "~", "<"), chars(">", "€", "£", "¥", "•")),
        SplitRow(
            listOf(Key(KeyType.TO_SYMBOLS2, "123", weight = 1.5f, duoWeight = 1f)) + chars(".", ",", "?", "!"),
            chars("'", "¿", "¡") + borrar,
        ),
        filaInferior(Key(KeyType.TO_LETTERS, "ABC", weight = 1.3f)),
    )

    fun filas(page: Page, conFilaNumeros: Boolean): List<SplitRow> = when (page) {
        Page.LETRAS -> if (conFilaNumeros) listOf(filaNumeros) + letras else letras
        Page.SIMBOLOS -> simbolos
        Page.SIMBOLOS2 -> simbolos2
    }

    /** Une las dos mitades para el modo completo; los dos espacios se funden en uno. */
    fun unir(fila: SplitRow): List<Key> {
        val l = fila.left
        val r = fila.right
        return if (l.lastOrNull()?.type == KeyType.SPACE && r.firstOrNull()?.type == KeyType.SPACE) {
            l.dropLast(1) + l.last().copy(weight = l.last().weight + r.first().weight) + r.drop(1)
        } else l + r
    }
}
