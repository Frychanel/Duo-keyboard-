import SwiftUI

@main
struct DuoKeyboardApp: App {
    var body: some Scene {
        WindowGroup {
            AjustesView()
                .preferredColorScheme(.dark)
        }
    }
}

/// Destino de texto para la vista previa dentro de la app.
final class VistaPreviaTexto: ObservableObject, TextTarget {
    @Published var texto = ""
    var antes: String? { texto }
    var autoMayus: UITextAutocapitalizationType { .sentences }
    var tipoTeclado: UIKeyboardType { .default }
    var tipoEnter: UIReturnKeyType { .default }
    func insertar(_ s: String) { texto += s }
    func borrar() { if !texto.isEmpty { texto.removeLast() } }
    func moverCursor(_ n: Int) {}
}
