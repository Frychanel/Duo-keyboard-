import Foundation
import SwiftUI

/// Contenedor compartido entre la app y el teclado (App Group).
enum AppGroup {
    static let id = "group.com.frychanel.duokeyboard"
    static var defaults: UserDefaults { UserDefaults(suiteName: id) ?? .standard }
    static var carpeta: URL {
        FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: id)
            ?? FileManager.default.temporaryDirectory
    }
}

protocol Ciclico: CaseIterable, Equatable where AllCases == [Self] {}
extension Ciclico {
    var siguiente: Self {
        let todos = Self.allCases
        let i = todos.firstIndex(of: self) ?? 0
        return todos[(i + 1) % todos.count]
    }
}

enum LayoutMode: String, Codable, Ciclico, Identifiable {
    case completo, duo, manoIzq, manoDer
    var id: String { rawValue }
    var etiqueta: String {
        switch self {
        case .completo: return "Completo"
        case .duo: return "Duo"
        case .manoIzq: return "Mano izq"
        case .manoDer: return "Mano der"
        }
    }
    var simbolo: String {
        switch self {
        case .completo: return "keyboard"
        case .duo: return "rectangle.split.2x1"
        case .manoIzq: return "hand.point.left"
        case .manoDer: return "hand.point.right"
        }
    }
}

enum ThemeId: String, Codable, Ciclico, Identifiable {
    case iosOscuro, iosClaro, cristal, neon, amoled, atardecer, menta, personal
    var id: String { rawValue }
    var etiqueta: String {
        switch self {
        case .iosOscuro: return "iOS Oscuro"
        case .iosClaro: return "iOS Claro"
        case .cristal: return "Cristal"
        case .neon: return "Neón"
        case .amoled: return "AMOLED"
        case .atardecer: return "Atardecer"
        case .menta: return "Menta"
        case .personal: return "Personal"
        }
    }
}

enum KeyStyle: String, Codable, CaseIterable, Identifiable {
    case solida, cristal, contorno, minimal
    var id: String { rawValue }
    var etiqueta: String {
        switch self {
        case .solida: return "Sólida"
        case .cristal: return "Cristal"
        case .contorno: return "Contorno"
        case .minimal: return "Minimal"
        }
    }
}

enum BackgroundType: String, Codable, CaseIterable, Identifiable {
    case tema, galeria, satelite
    var id: String { rawValue }
    var etiqueta: String {
        switch self {
        case .tema: return "Tema"
        case .galeria: return "Galería"
        case .satelite: return "Satélite"
        }
    }
}

/// Todo lo que se puede personalizar.
struct KeyboardConfig: Codable, Equatable {
    // Diseño
    var modoTeclado: LayoutMode = .duo
    var duoEnHorizontal = true
    var alturaEscala = 1.0
    var separacionDuo = 0.14
    var anguloDuo = 4.0
    var curva = 0.0
    var anchoUnaMano = 0.82
    var elevacion = 0.0
    var filaNumeros = false
    var barraHerramientas = true
    // Estilo
    var tema: ThemeId = .iosOscuro
    var estiloTecla: KeyStyle = .solida
    var colorFondo = 0x1C1C1E
    var colorTecla = 0x3A3A3C
    var colorTexto = 0xFFFFFF
    var colorAcento = 0x0A84FF
    // Teclas
    var radioTecla = 9.0
    var espaciado = 6.0
    var opacidadTecla = 1.0
    var escalaTexto = 1.0
    var sombra = true
    var bordes = false
    var popup = true
    // Fondo
    var fondoTipo: BackgroundType = .tema
    var fondoOculto = false
    var fondoOscurecer = 0.25
    var fondoDesenfoque = 0.0
    var fondoVersion = 0
    var sateliteNombre = ""
    // Escritura
    var vibracion = true
    var fuerzaVibracion = 0.5
    var sonido = false
    var mayusAuto = true
    var puntoDobleEspacio = true
    var cursorEnEspacio = true
    var acentosLargos = true
}

/// Lee y guarda la configuración en el App Group (la ven la app y el teclado).
final class ConfigStore: ObservableObject {
    private static let clave = "config_json"

    @Published var config: KeyboardConfig {
        didSet { if config != oldValue { guardar() } }
    }

    init() { config = ConfigStore.leer() }

    static func leer() -> KeyboardConfig {
        guard let datos = AppGroup.defaults.data(forKey: clave),
              let c = try? JSONDecoder().decode(KeyboardConfig.self, from: datos) else { return KeyboardConfig() }
        return c
    }

    func recargar() {
        let c = ConfigStore.leer()
        if c != config { config = c }
    }

    func restablecer() {
        var c = KeyboardConfig()
        c.fondoVersion = config.fondoVersion + 1
        config = c
    }

    private func guardar() {
        if let datos = try? JSONEncoder().encode(config) {
            AppGroup.defaults.set(datos, forKey: ConfigStore.clave)
        }
    }
}

// MARK: - Premium

enum PremiumFlag {
    static let productoID = "duokeyboard_premium"
    private static let clave = "premium_activo"
    /// La app lo escribe tras verificar la compra; el teclado solo lo lee.
    static var activo: Bool {
        get { AppGroup.defaults.bool(forKey: clave) }
        set { AppGroup.defaults.set(newValue, forKey: clave) }
    }
}

extension ThemeId {
    var esPremium: Bool { self != .iosOscuro && self != .iosClaro }
    func siguienteDisponible(premium: Bool) -> ThemeId {
        var t = siguiente
        while !premium && t.esPremium { t = t.siguiente }
        return t
    }
}

extension KeyStyle { var esPremium: Bool { self == .contorno || self == .minimal } }
extension BackgroundType { var esPremium: Bool { self != .tema } }

extension KeyboardConfig {
    /// Lo que el teclado realmente usa: sin Premium, las opciones de pago vuelven a las gratis.
    func efectiva(premium: Bool) -> KeyboardConfig {
        guard !premium else { return self }
        var c = self
        if c.tema.esPremium { c.tema = .iosOscuro }
        if c.estiloTecla.esPremium { c.estiloTecla = .solida }
        if c.fondoTipo.esPremium { c.fondoTipo = .tema }
        return c
    }
}
