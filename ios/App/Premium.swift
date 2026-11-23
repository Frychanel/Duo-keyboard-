import Foundation
import StoreKit

/// Compra única "DuoKeyboard Premium" con StoreKit 2.
/// En App Store Connect crea una compra dentro de la app "No consumible" con el ID `duokeyboard_premium`.
@MainActor
final class PremiumStore: ObservableObject {
    @Published private(set) var activo = PremiumFlag.activo
    @Published private(set) var producto: Product?
    @Published var comprando = false
    @Published var aviso: String?

    private var escucha: Task<Void, Never>?

    init() {
        escucha = Task { [weak self] in
            for await resultado in Transaction.updates {
                if case .verified(let t) = resultado {
                    await t.finish()
                    await self?.actualizar()
                }
            }
        }
        Task {
            await cargarProducto()
            await actualizar()
        }
    }

    deinit { escucha?.cancel() }

    var precio: String? { producto?.displayPrice }

    func cargarProducto() async {
        producto = try? await Product.products(for: [PremiumFlag.productoID]).first
    }

    func actualizar() async {
        var tiene = false
        for await resultado in Transaction.currentEntitlements {
            if case .verified(let t) = resultado, t.productID == PremiumFlag.productoID, t.revocationDate == nil {
                tiene = true
            }
        }
        activo = tiene
        PremiumFlag.activo = tiene
    }

    func comprar() async {
        if producto == nil { await cargarProducto() }
        guard let producto else {
            aviso = "La tienda aún no está disponible. Intenta más tarde."
            return
        }
        comprando = true
        defer { comprando = false }
        do {
            let resultado = try await producto.purchase()
            switch resultado {
            case .success(let verificacion):
                if case .verified(let t) = verificacion {
                    await t.finish()
                    await actualizar()
                    aviso = "¡Premium desbloqueado! ✓"
                }
            case .pending:
                aviso = "Tu compra está pendiente de aprobación."
            case .userCancelled:
                break
            @unknown default:
                break
            }
        } catch {
            aviso = "No se pudo completar la compra."
        }
    }

    func restaurar() async {
        aviso = "Buscando compras anteriores…"
        try? await AppStore.sync()
        await actualizar()
        aviso = activo ? "Compra restaurada ✓" : "No encontramos compras anteriores."
    }
}
