import UIKit
import AudioToolbox

enum Page { case letras, simbolos, simbolos2 }
enum ShiftState { case off, once, lock }

/// A dónde va el texto: el campo real (teclado) o la vista previa (app).
protocol TextTarget: AnyObject {
    var antes: String? { get }
    var autoMayus: UITextAutocapitalizationType { get }
    var tipoTeclado: UIKeyboardType { get }
    var tipoEnter: UIReturnKeyType { get }
    func insertar(_ s: String)
    func borrar()
    func moverCursor(_ n: Int)
}

final class KeyboardState: ObservableObject {
    @Published var page: Page = .letras
    @Published var shift: ShiftState = .off
    @Published var acentos: [String] = []
    @Published private(set) var etiquetaEnter: String?

    weak var target: TextTarget?
    let store: ConfigStore
    var alCambiarTeclado: () -> Void = {}
    var alOcultar: () -> Void = {}
    var muestraGlobo = false

    private let impacto = UIImpactFeedbackGenerator(style: .light)
    private var ultimoShift = Date.distantPast
    private var ultimoEspacio = Date.distantPast
    private var cfg: KeyboardConfig { store.config }

    init(store: ConfigStore) { self.store = store }

    func textoTecla(_ k: Key) -> String {
        k.type == .char && page == .letras && shift != .off ? k.label.uppercased() : k.label
    }

    // MARK: Ciclo de vida

    func alIniciar() {
        guard let t = target else { return }
        switch t.tipoTeclado {
        case .numberPad, .decimalPad, .phonePad, .numbersAndPunctuation, .asciiCapableNumberPad: page = .simbolos
        default: page = .letras
        }
        shift = .off
        acentos = []
        actualizarEnter()
        mayusAuto()
    }

    func alCambiarTexto() {
        actualizarEnter()
        if acentos.isEmpty { mayusAuto() }
    }

    // MARK: Teclas

    func alPresionar(_ k: Key) {
        retro(k.type)
        if k.type == .delete { borrar() }
    }

    func alTocar(_ k: Key) {
        switch k.type {
        case .char: escribir(textoTecla(k))
        case .space: espacio()
        case .enter:
            target?.insertar("\n")
            mayusAuto()
        case .shift: alternarShift()
        case .toSymbols: page = .simbolos; acentos = []
        case .toLetters: page = .letras; mayusAuto()
        case .toSymbols2: page = page == .simbolos2 ? .simbolos : .simbolos2
        case .delete: break
        }
    }

    /// true si la pulsación larga hizo algo (entonces se ignora el toque).
    func alMantener(_ k: Key) -> Bool {
        guard k.type == .char, cfg.acentosLargos, !k.alts.isEmpty else { return false }
        let mayus = page == .letras && shift != .off
        acentos = ([k.label] + k.alts).map { mayus ? $0.uppercased() : $0 }
        retro(.shift)
        return true
    }

    func alRepetir(_ k: Key) { if k.type == .delete { borrar() } }
    func moverCursor(_ n: Int) { target?.moverCursor(n) }

    func elegirAcento(_ s: String) {
        escribir(s)
        acentos = []
    }

    func cerrarAcentos() { acentos = [] }

    // MARK: Lógica

    private func escribir(_ s: String) {
        target?.insertar(s)
        if shift == .once { shift = .off }
        acentos = []
        mayusAuto()
    }

    private func espacio() {
        let ahora = Date()
        if cfg.puntoDobleEspacio, ahora.timeIntervalSince(ultimoEspacio) < 0.7, let a = target?.antes, a.count >= 2 {
            let dos = Array(a.suffix(2))
            if dos[1] == " " && (dos[0].isLetter || dos[0].isNumber) {
                target?.borrar()
                target?.insertar(". ")
                ultimoEspacio = .distantPast
                mayusAuto()
                return
            }
        }
        target?.insertar(" ")
        ultimoEspacio = ahora
        if page != .letras { page = .letras }
        if shift == .once { shift = .off }
        mayusAuto()
    }

    private func borrar() {
        target?.borrar()
        mayusAuto()
    }

    private func alternarShift() {
        let ahora = Date()
        if shift == .lock {
            shift = .off
        } else if ahora.timeIntervalSince(ultimoShift) < 0.35 {
            shift = .lock
        } else {
            shift = shift == .off ? .once : .off
        }
        ultimoShift = ahora
    }

    private func mayusAuto() {
        guard cfg.mayusAuto, shift != .lock, page == .letras, let t = target else { return }
        if t.tipoTeclado == .emailAddress || t.tipoTeclado == .URL { return }
        let antes = t.antes ?? ""
        let caps: Bool
        switch t.autoMayus {
        case .none: return
        case .allCharacters: caps = true
        case .words: caps = antes.isEmpty || antes.last == " " || antes.last == "\n"
        default:
            let fin: Set<Character> = [".", "!", "?"]
            let sinEspacios = antes.reversed().drop(while: { $0 == " " })
            if antes.isEmpty || antes.last == "\n" || sinEspacios.isEmpty {
                caps = true
            } else if antes.last == " ", let ultimo = sinEspacios.first {
                caps = fin.contains(ultimo)
            } else {
                caps = false
            }
        }
        let nuevo: ShiftState = caps ? .once : .off
        if shift != nuevo { shift = nuevo }
    }

    private func actualizarEnter() {
        let nueva: String?
        switch target?.tipoEnter ?? .default {
        case .go: nueva = "ir"
        case .google, .search, .yahoo: nueva = "buscar"
        case .send: nueva = "enviar"
        case .next: nueva = "sig."
        case .done: nueva = "listo"
        case .join: nueva = "unirse"
        case .route: nueva = "ruta"
        case .continue: nueva = "seguir"
        default: nueva = nil
        }
        if nueva != etiquetaEnter { etiquetaEnter = nueva }
    }

    /// Vibración (requiere "Acceso total") y clic.
    private func retro(_ tipo: KeyType) {
        if cfg.vibracion { impacto.impactOccurred(intensity: CGFloat(cfg.fuerzaVibracion)) }
        if cfg.sonido {
            AudioServicesPlaySystemSound(tipo == .delete ? 1155 : (tipo == .space ? 1156 : 1104))
        }
    }
}
