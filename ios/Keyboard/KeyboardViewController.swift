import UIKit
import SwiftUI
import Combine

/// Datos de la pantalla que cambian (ancho, orientación, imagen de fondo).
final class Entorno: ObservableObject {
    @Published var ancho: CGFloat = 0
    @Published var horizontal = false
    @Published var fondo: UIImage?
    @Published var premium = false
}

struct RaizTeclado: View {
    @ObservedObject var entorno: Entorno
    let state: KeyboardState
    let store: ConfigStore

    var body: some View {
        KeyboardView(state: state, store: store, fondo: entorno.fondo,
                     horizontal: entorno.horizontal, ancho: entorno.ancho, premium: entorno.premium)
    }
}

/// Conecta el teclado con el campo de texto de la app donde se escribe.
final class ProxyTarget: TextTarget {
    weak var vc: UIInputViewController?
    init(_ vc: UIInputViewController) { self.vc = vc }

    private var proxy: UITextDocumentProxy? { vc?.textDocumentProxy }

    var antes: String? { proxy?.documentContextBeforeInput }
    var autoMayus: UITextAutocapitalizationType { proxy?.autocapitalizationType ?? .sentences }
    var tipoTeclado: UIKeyboardType { proxy?.keyboardType ?? .default }
    var tipoEnter: UIReturnKeyType { proxy?.returnKeyType ?? .default }

    func insertar(_ s: String) { proxy?.insertText(s) }
    func borrar() { proxy?.deleteBackward() }
    func moverCursor(_ n: Int) { proxy?.adjustTextPosition(byCharacterOffset: n) }
}

final class KeyboardViewController: UIInputViewController {
    private let store = ConfigStore()
    private lazy var estado = KeyboardState(store: store)
    private lazy var destino = ProxyTarget(self)
    private let entorno = Entorno()
    private var altura: NSLayoutConstraint?
    private var claveFondo = ""
    private var bolsa = Set<AnyCancellable>()

    override func viewDidLoad() {
        super.viewDidLoad()
        estado.target = destino
        estado.alCambiarTeclado = { [weak self] in self?.advanceToNextInputMode() }
        estado.alOcultar = { [weak self] in self?.dismissKeyboard() }

        let host = UIHostingController(rootView: RaizTeclado(entorno: entorno, state: estado, store: store))
        host.view.backgroundColor = .clear
        if #available(iOS 16.4, *) { host.safeAreaRegions = [] }
        addChild(host)
        view.addSubview(host.view)
        host.view.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            host.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            host.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            host.view.topAnchor.constraint(equalTo: view.topAnchor),
            host.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
        host.didMove(toParent: self)

        let h = view.heightAnchor.constraint(equalToConstant: 260)
        h.priority = UILayoutPriority(999)
        h.isActive = true
        altura = h

        estado.objectWillChange
            .merge(with: store.objectWillChange)
            .receive(on: RunLoop.main)
            .sink { [weak self] _ in self?.actualizar() }
            .store(in: &bolsa)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        store.recargar()
        entorno.premium = PremiumFlag.activo
        estado.muestraGlobo = needsInputModeSwitchKey
        estado.alIniciar()
        actualizar()
    }

    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        actualizar()
    }

    override func textDidChange(_ textInput: UITextInput?) {
        super.textDidChange(textInput)
        estado.alCambiarTexto()
    }

    private func actualizar() {
        let ancho = view.bounds.width
        guard ancho > 0 else { return }
        let horizontal = traitCollection.verticalSizeClass == .compact
        if entorno.ancho != ancho { entorno.ancho = ancho }
        if entorno.horizontal != horizontal { entorno.horizontal = horizontal }

        let c = store.config
        let clave = "\(c.fondoTipo.rawValue)-\(c.fondoVersion)"
        if clave != claveFondo {
            claveFondo = clave
            entorno.fondo = Fondos.cargar(c.fondoTipo)
        }

        let m = Medidas(config: c, page: estado.page, horizontal: horizontal, ancho: ancho,
                        conBarra: c.barraHerramientas || !estado.acentos.isEmpty)
        if let a = altura, abs(a.constant - m.total) > 0.5 { a.constant = m.total }
    }
}
