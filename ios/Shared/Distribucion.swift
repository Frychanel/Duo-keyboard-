import CoreGraphics

enum KeyType { case char, shift, delete, enter, space, toSymbols, toLetters, toSymbols2 }

struct Key: Equatable {
    var type: KeyType
    var label: String = ""
    var weight: CGFloat = 1
    var duoWeight: CGFloat? = nil
    var alts: [String] = []

    var pesoDuo: CGFloat { duoWeight ?? weight }
    func peso(duo: Bool) -> CGFloat { duo ? pesoDuo : weight }
}

/// Cada fila viene partida en mitad izquierda y derecha (modo Duo).
struct SplitRow {
    var left: [Key]
    var right: [Key]
}

enum Layouts {
    private static let alternativas: [String: [String]] = [
        "a": ["á", "à", "ä", "â", "ã", "å", "æ"],
        "e": ["é", "è", "ë", "ê", "€"],
        "i": ["í", "ì", "ï", "î"],
        "o": ["ó", "ò", "ö", "ô", "õ", "ø", "º"],
        "u": ["ú", "ù", "ü", "û"],
        "n": ["ñ"],
        "c": ["ç", "©"],
        "s": ["ß", "$"],
        "y": ["ý", "ÿ"],
        ".": ["…", "·"],
        ",": [";", ":"],
        "?": ["¿"],
        "!": ["¡"],
        "-": ["–", "—", "_"],
        "$": ["€", "£", "¥", "¢"],
        "'": ["‘", "’", "`"],
        "\"": ["“", "”", "«", "»"],
        "0": ["°"],
    ]

    private static func c(_ s: String) -> Key { Key(type: .char, label: s, alts: alternativas[s] ?? []) }
    private static func cs(_ s: String...) -> [Key] { s.map(c) }

    private static let borrar = Key(type: .delete, weight: 1.5, duoWeight: 2)
    private static let espacio = Key(type: .space, label: " ", weight: 2.7)

    private static func inferior(_ modo: Key) -> SplitRow {
        SplitRow(left: [modo, c(","), espacio], right: [espacio, c("."), Key(type: .enter, weight: 1.3)])
    }

    private static let numeros = SplitRow(left: cs("1", "2", "3", "4", "5"), right: cs("6", "7", "8", "9", "0"))

    private static let letras: [SplitRow] = [
        SplitRow(left: cs("q", "w", "e", "r", "t"), right: cs("y", "u", "i", "o", "p")),
        SplitRow(left: cs("a", "s", "d", "f", "g"), right: cs("h", "j", "k", "l", "ñ")),
        SplitRow(left: [Key(type: .shift, weight: 1.5, duoWeight: 1)] + cs("z", "x", "c", "v"),
                 right: cs("b", "n", "m") + [borrar]),
        inferior(Key(type: .toSymbols, label: "123", weight: 1.3)),
    ]

    private static let simbolos: [SplitRow] = [
        numeros,
        SplitRow(left: cs("-", "/", ":", ";", "("), right: cs(")", "$", "&", "@", "\"")),
        SplitRow(left: [Key(type: .toSymbols2, label: "#+=", weight: 1.5, duoWeight: 1)] + cs(".", ",", "?", "!"),
                 right: cs("'", "*", "_") + [borrar]),
        inferior(Key(type: .toLetters, label: "ABC", weight: 1.3)),
    ]

    private static let simbolos2: [SplitRow] = [
        SplitRow(left: cs("[", "]", "{", "}", "#"), right: cs("%", "^", "*", "+", "=")),
        SplitRow(left: cs("_", "\\", "|", "~", "<"), right: cs(">", "€", "£", "¥", "•")),
        SplitRow(left: [Key(type: .toSymbols2, label: "123", weight: 1.5, duoWeight: 1)] + cs(".", ",", "?", "!"),
                 right: cs("'", "¿", "¡") + [borrar]),
        inferior(Key(type: .toLetters, label: "ABC", weight: 1.3)),
    ]

    static func filas(_ page: Page, numeros conNumeros: Bool) -> [SplitRow] {
        switch page {
        case .letras: return conNumeros ? [numeros] + letras : letras
        case .simbolos: return simbolos
        case .simbolos2: return simbolos2
        }
    }

    /// Une las dos mitades; los dos espacios se funden en uno.
    static func unir(_ f: SplitRow) -> [Key] {
        if let l = f.left.last, l.type == .space, let r = f.right.first, r.type == .space {
            var esp = l
            esp.weight = l.weight + r.weight
            return Array(f.left.dropLast()) + [esp] + Array(f.right.dropFirst())
        }
        return f.left + f.right
    }
}
