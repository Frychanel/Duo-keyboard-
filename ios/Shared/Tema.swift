import SwiftUI
import UIKit

extension Color {
    /// 0xAARRGGBB
    init(argb: UInt32) {
        self.init(.sRGB,
                  red: Double((argb >> 16) & 0xFF) / 255,
                  green: Double((argb >> 8) & 0xFF) / 255,
                  blue: Double(argb & 0xFF) / 255,
                  opacity: Double((argb >> 24) & 0xFF) / 255)
    }

    /// 0xRRGGBB
    init(rgb: Int) {
        self.init(argb: 0xFF00_0000 | UInt32(rgb & 0xFFFFFF))
    }

    var rgbInt: Int {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        UIColor(self).getRed(&r, green: &g, blue: &b, alpha: &a)
        let c = { (v: CGFloat) in Int((min(max(v, 0), 1) * 255).rounded()) }
        return (c(r) << 16) | (c(g) << 8) | c(b)
    }
}

private func mezclar(_ a: Int, _ b: Int, _ t: Double) -> Int {
    func canal(_ v: Int, _ s: Int) -> Double { Double((v >> s) & 0xFF) }
    func m(_ s: Int) -> Int { Int(canal(a, s) + (canal(b, s) - canal(a, s)) * t) }
    return (m(16) << 16) | (m(8) << 8) | m(0)
}

private func luminancia(_ v: Int) -> Double {
    (0.299 * Double((v >> 16) & 0xFF) + 0.587 * Double((v >> 8) & 0xFF) + 0.114 * Double(v & 0xFF)) / 255
}

struct Tema {
    let fondo: [Color]
    let tecla: Color
    let especial: Color
    let activa: Color
    let texto: Color
    let textoEspecial: Color
    let textoActiva: Color
    let acento: Color
    let textoAcento: Color
    let presionada: Color
    let borde: Color
    let sombra: Color
    let iconos: Color
    let burbuja: Color
    let textoBurbuja: Color

    static func para(_ c: KeyboardConfig, _ id: ThemeId? = nil) -> Tema {
        let x = Color.init(argb:)
        switch id ?? c.tema {
        case .iosClaro:
            return Tema(fondo: [x(0xFFD1D3D9)], tecla: .white, especial: x(0xFFABB0BC), activa: .white,
                        texto: .black, textoEspecial: .black, textoActiva: .black, acento: x(0xFF007AFF), textoAcento: .white,
                        presionada: x(0xFFB8BCC6), borde: x(0x1A000000), sombra: x(0x59898A8D), iconos: x(0xFF3C3C43),
                        burbuja: .white, textoBurbuja: .black)
        case .iosOscuro:
            return Tema(fondo: [x(0xFF202022)], tecla: x(0xFF56565A), especial: x(0xFF38383B), activa: x(0xFFE5E5EA),
                        texto: .white, textoEspecial: .white, textoActiva: .black, acento: x(0xFF0A84FF), textoAcento: .white,
                        presionada: x(0xFF77777C), borde: x(0x26FFFFFF), sombra: x(0xFF0E0E0F), iconos: x(0xFFD1D1D6),
                        burbuja: x(0xFF6C6C70), textoBurbuja: .white)
        case .cristal:
            return Tema(fondo: [x(0xFF0F172A), x(0xFF1E3A8A)], tecla: x(0x2EFFFFFF), especial: x(0x17FFFFFF), activa: x(0xE6FFFFFF),
                        texto: .white, textoEspecial: .white, textoActiva: x(0xFF0F172A), acento: x(0xFF38BDF8), textoAcento: .black,
                        presionada: x(0x59FFFFFF), borde: x(0x40FFFFFF), sombra: x(0x33000000), iconos: x(0xFFE0F2FE),
                        burbuja: x(0xFF334E8C), textoBurbuja: .white)
        case .neon:
            return Tema(fondo: [x(0xFF07070F), x(0xFF140A26)], tecla: x(0xFF15152A), especial: x(0xFF0E0E1E), activa: x(0xFF00F5FF),
                        texto: x(0xFF00F5FF), textoEspecial: x(0xFFFF2BD6), textoActiva: x(0xFF07070F), acento: x(0xFFFF2BD6), textoAcento: .white,
                        presionada: x(0xFF2A2A55), borde: x(0x8000F5FF), sombra: x(0x66FF2BD6), iconos: x(0xFF00F5FF),
                        burbuja: x(0xFF1D1D3A), textoBurbuja: x(0xFF00F5FF))
        case .amoled:
            return Tema(fondo: [.black], tecla: x(0xFF171717), especial: x(0xFF0D0D0D), activa: x(0xFFEDEDED),
                        texto: x(0xFFEDEDED), textoEspecial: x(0xFFBDBDBD), textoActiva: .black, acento: x(0xFF30D158), textoAcento: .black,
                        presionada: x(0xFF2E2E2E), borde: x(0x1FFFFFFF), sombra: .clear, iconos: x(0xFFBDBDBD),
                        burbuja: x(0xFF262626), textoBurbuja: .white)
        case .atardecer:
            return Tema(fondo: [x(0xFFFF7E5F), x(0xFF6A3093)], tecla: x(0x40FFFFFF), especial: x(0x24FFFFFF), activa: x(0xF2FFFFFF),
                        texto: .white, textoEspecial: .white, textoActiva: x(0xFF6A3093), acento: x(0xFFFFD166), textoAcento: .black,
                        presionada: x(0x73FFFFFF), borde: x(0x4DFFFFFF), sombra: x(0x26000000), iconos: .white,
                        burbuja: x(0xFFB4507A), textoBurbuja: .white)
        case .menta:
            return Tema(fondo: [x(0xFFE6F7EF), x(0xFFCDEFE0)], tecla: .white, especial: x(0xFFB5E3CE), activa: x(0xFF0F3D2E),
                        texto: x(0xFF0F3D2E), textoEspecial: x(0xFF0F3D2E), textoActiva: .white, acento: x(0xFF10B981), textoAcento: .white,
                        presionada: x(0xFFC9EBDB), borde: x(0x1A0F3D2E), sombra: x(0x400F3D2E), iconos: x(0xFF0F3D2E),
                        burbuja: .white, textoBurbuja: x(0xFF0F3D2E))
        case .personal:
            let activa = mezclar(c.colorTecla, c.colorTexto, 0.85)
            return Tema(fondo: [Color(rgb: c.colorFondo)], tecla: Color(rgb: c.colorTecla),
                        especial: Color(rgb: mezclar(c.colorTecla, c.colorFondo, 0.5)), activa: Color(rgb: activa),
                        texto: Color(rgb: c.colorTexto), textoEspecial: Color(rgb: c.colorTexto),
                        textoActiva: luminancia(activa) > 0.5 ? .black : .white,
                        acento: Color(rgb: c.colorAcento), textoAcento: luminancia(c.colorAcento) > 0.55 ? .black : .white,
                        presionada: Color(rgb: mezclar(c.colorTecla, c.colorTexto, 0.25)),
                        borde: Color(rgb: c.colorTexto).opacity(0.18), sombra: Color.black.opacity(0.3),
                        iconos: Color(rgb: c.colorTexto).opacity(0.85),
                        burbuja: Color(rgb: mezclar(c.colorTecla, c.colorTexto, 0.12)), textoBurbuja: Color(rgb: c.colorTexto))
        }
    }
}
