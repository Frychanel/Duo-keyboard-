import SwiftUI
import PhotosUI

private enum Seccion: String, CaseIterable, Identifiable {
    case activar = "Activar", diseno = "Diseño", estilo = "Estilo", fondo = "Fondo", teclas = "Teclas", escritura = "Escritura"
    var id: String { rawValue }
}

struct AjustesView: View {
    @StateObject private var store: ConfigStore
    @StateObject private var estado: KeyboardState
    @StateObject private var prueba = VistaPreviaTexto()
    @StateObject private var premium = PremiumStore()
    @State private var seccion: Seccion = .activar
    @State private var fondo: UIImage?

    init() {
        let s = ConfigStore()
        _store = StateObject(wrappedValue: s)
        _estado = StateObject(wrappedValue: KeyboardState(store: s))
    }

    private var anchoPrevia: CGFloat { UIScreen.main.bounds.width - 24 }

    var body: some View {
        NavigationStack {
            VStack(spacing: 10) {
                Text(prueba.texto.isEmpty ? "Toca las teclas para probar…" : prueba.texto)
                    .foregroundColor(prueba.texto.isEmpty ? .secondary : .primary)
                    .lineLimit(2)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(12)
                    .background(RoundedRectangle(cornerRadius: 12).fill(Color(.secondarySystemBackground)))
                    .padding(.horizontal, 12)

                KeyboardView(state: estado, store: store, fondo: fondo, horizontal: false,
                             ancho: anchoPrevia, esVistaPrevia: true)
                    .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(Seccion.allCases) { s in
                            Button { seccion = s } label: {
                                Text(s.rawValue)
                                    .font(.subheadline.weight(.medium))
                                    .padding(.horizontal, 14).padding(.vertical, 8)
                                    .background(Capsule().fill(seccion == s ? Color.accentColor : Color(.secondarySystemBackground)))
                                    .foregroundColor(seccion == s ? .white : .primary)
                            }
                        }
                    }
                    .padding(.horizontal, 12)
                }

                Form { contenidoSeccion }
            }
            .navigationTitle("DuoKeyboard")
        }
        .onAppear {
            estado.target = prueba
            estado.alIniciar()
        }
        .task(id: "\(store.config.fondoTipo.rawValue)-\(store.config.fondoVersion)") {
            fondo = Fondos.cargar(store.config.fondoTipo)
        }
    }

    @ViewBuilder
    private var contenidoSeccion: some View {
        switch seccion {
        case .activar: SeccionActivar()
        case .diseno: SeccionDiseno(store: store)
        case .estilo: SeccionEstilo(store: store, premium: premium)
        case .fondo: SeccionFondo(store: store, premium: premium)
        case .teclas: SeccionTeclas(store: store)
        case .escritura: SeccionEscritura(store: store)
        }
    }
}

// MARK: - Componentes

private struct FilaDeslizador: View {
    let titulo: String
    @Binding var valor: Double
    let rango: ClosedRange<Double>
    let formato: (Double) -> String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(titulo)
                Spacer()
                Text(formato(valor)).foregroundColor(.accentColor).monospacedDigit()
            }
            Slider(value: $valor, in: rango)
        }
    }
}

/// Marca con ★ las opciones de pago mientras no se haya comprado Premium.
private func estrella(_ texto: String, _ dePago: Bool, _ premium: Bool) -> String {
    dePago && !premium ? "\(texto) ★" : texto
}

private struct TarjetaPremium: View {
    @ObservedObject var premium: PremiumStore
    var body: some View {
        if !premium.activo {
            Section {
                Text("Desbloquea 6 temas más (incluido Personal), fondos con tus fotos e imágenes satelitales, y los estilos Contorno y Minimal.")
                Text("Pruébalos gratis aquí en la vista previa; en el teclado se activan al desbloquear.")
                    .font(.footnote).foregroundColor(.secondary)
                Button {
                    Task { await premium.comprar() }
                } label: {
                    HStack {
                        Spacer()
                        if premium.comprando { ProgressView() }
                        Text(premium.precio.map { "Desbloquear · \($0)" } ?? "Desbloquear Premium").bold()
                        Spacer()
                    }
                }
                .disabled(premium.comprando)
                Button("Restaurar compra") { Task { await premium.restaurar() } }
                if let aviso = premium.aviso { Text(aviso).font(.footnote).foregroundColor(.accentColor) }
            } header: {
                Text("★ DuoKeyboard Premium")
            }
        } else if let aviso = premium.aviso {
            Section { Text(aviso).foregroundColor(.accentColor) }
        }
    }
}

private func pct(_ v: Double) -> String { "\(Int((v * 100).rounded()))%" }
private func dp(_ v: Double) -> String { "\(Int(v.rounded())) pt" }

// MARK: - Secciones

private struct SeccionActivar: View {
    @State private var texto = ""
    var body: some View {
        Section("Cómo activarlo") {
            Label("Abre Ajustes → General → Teclado → Teclados", systemImage: "1.circle")
            Label("Toca «Agregar nuevo teclado» y elige DuoKeyboard", systemImage: "2.circle")
            Label("Toca DuoKeyboard y activa «Permitir acceso total» para la vibración", systemImage: "3.circle")
            Label("En cualquier app, mantén el globo 🌐 y elige DuoKeyboard", systemImage: "4.circle")
            Button("Abrir Ajustes") {
                if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
            }
        }
        Section("Pruébalo") {
            TextField("Escribe aquí…", text: $texto, axis: .vertical)
                .lineLimit(3...6)
        }
        Section {
            Text("DuoKeyboard no guarda ni envía lo que escribes. «Acceso total» solo se usa para la vibración y el sonido.")
                .font(.footnote).foregroundColor(.secondary)
        }
    }
}

private struct SeccionDiseno: View {
    @ObservedObject var store: ConfigStore
    var body: some View {
        Section("Modo") {
            Picker("Modo", selection: $store.config.modoTeclado) {
                ForEach(LayoutMode.allCases) { Text($0.etiqueta).tag($0) }
            }
            .pickerStyle(.segmented)
            Toggle("Duo automático en horizontal", isOn: $store.config.duoEnHorizontal)
        }
        Section("Duo · dividido") {
            FilaDeslizador(titulo: "Separación central", valor: $store.config.separacionDuo, rango: 0.04...0.40, formato: pct)
            FilaDeslizador(titulo: "Inclinación", valor: $store.config.anguloDuo, rango: 0...15) { "\(Int($0.rounded()))°" }
            FilaDeslizador(titulo: "Curvatura", valor: $store.config.curva, rango: 0...24, formato: dp)
        }
        Section("Tamaño y posición") {
            FilaDeslizador(titulo: "Altura", valor: $store.config.alturaEscala, rango: 0.7...1.45, formato: pct)
            FilaDeslizador(titulo: "Ancho en una mano", valor: $store.config.anchoUnaMano, rango: 0.6...0.95, formato: pct)
            FilaDeslizador(titulo: "Elevar desde abajo", valor: $store.config.elevacion, rango: 0...60, formato: dp)
            Toggle("Fila de números", isOn: $store.config.filaNumeros)
            Toggle("Barra de herramientas", isOn: $store.config.barraHerramientas)
        }
    }
}

private struct SeccionEstilo: View {
    @ObservedObject var store: ConfigStore
    @ObservedObject var premium: PremiumStore

    private func color(_ ruta: WritableKeyPath<KeyboardConfig, Int>) -> Binding<Color> {
        Binding(get: { Color(rgb: store.config[keyPath: ruta]) },
                set: { store.config[keyPath: ruta] = $0.rgbInt })
    }

    var body: some View {
        TarjetaPremium(premium: premium)
        Section("Tema") {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 92), spacing: 10)], spacing: 12) {
                ForEach(ThemeId.allCases) { id in
                    let t = Tema.para(store.config, id)
                    let elegido = store.config.tema == id
                    Button { store.config.tema = id } label: {
                        VStack(spacing: 4) {
                            ZStack {
                                LinearGradient(colors: t.fondo, startPoint: .top, endPoint: .bottom)
                                HStack(spacing: 4) {
                                    ForEach(Array([t.tecla, t.tecla, t.especial, t.acento].enumerated()), id: \.offset) { _, col in
                                        RoundedRectangle(cornerRadius: 4).fill(col).frame(width: 15, height: 22)
                                    }
                                }
                            }
                            .frame(height: 56)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                            .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .stroke(elegido ? Color.accentColor : Color.white.opacity(0.12), lineWidth: elegido ? 2.5 : 1))
                            Text(estrella(id.etiqueta, id.esPremium, premium.activo)).font(.caption).foregroundColor(elegido ? .accentColor : .primary)
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.vertical, 4)
        }
        Section("Estilo de tecla") {
            Picker("Estilo", selection: $store.config.estiloTecla) {
                ForEach(KeyStyle.allCases) { Text(estrella($0.etiqueta, $0.esPremium, premium.activo)).tag($0) }
            }
            .pickerStyle(.segmented)
        }
        if store.config.tema == .personal {
            Section("Colores personalizados") {
                ColorPicker("Fondo", selection: color(\.colorFondo), supportsOpacity: false)
                ColorPicker("Teclas", selection: color(\.colorTecla), supportsOpacity: false)
                ColorPicker("Texto", selection: color(\.colorTexto), supportsOpacity: false)
                ColorPicker("Acento", selection: color(\.colorAcento), supportsOpacity: false)
            }
        } else {
            Section { Text("Elige «Personal» para escoger tus propios colores.").font(.footnote).foregroundColor(.secondary) }
        }
    }
}

private struct SeccionFondo: View {
    @ObservedObject var store: ConfigStore
    @ObservedObject var premium: PremiumStore
    @State private var foto: PhotosPickerItem?
    @State private var descargando = false
    @State private var mensaje: String?
    @State private var latTxt = "19.4326"
    @State private var lonTxt = "-99.1332"
    @State private var zoom = 13

    var body: some View {
        TarjetaPremium(premium: premium)
        Section("Tipo de fondo") {
            Picker("Tipo", selection: $store.config.fondoTipo) {
                ForEach(BackgroundType.allCases) { Text(estrella($0.etiqueta, $0.esPremium, premium.activo)).tag($0) }
            }
            .pickerStyle(.segmented)
            Toggle("Esconder fondo", isOn: $store.config.fondoOculto)
        }
        Section("Foto de tu galería") {
            PhotosPicker(selection: $foto, matching: .images) {
                Label("Elegir foto", systemImage: "photo.on.rectangle")
            }
        }
        .onChange(of: foto) { item in
            guard let item else { return }
            Task {
                mensaje = "Procesando foto…"
                if let data = try? await item.loadTransferable(type: Data.self), Fondos.guardarGaleria(data) {
                    store.config.fondoTipo = .galeria
                    store.config.fondoOculto = false
                    store.config.fondoVersion += 1
                    mensaje = "Foto aplicada ✓"
                } else {
                    mensaje = "No se pudo usar esa imagen."
                }
            }
        }
        Section {
            ForEach(Fondos.lugares) { lugar in
                Button {
                    descargar(lugar.nombre, lugar.lat, lugar.lon, lugar.zoom)
                } label: {
                    HStack {
                        Text(lugar.nombre).foregroundColor(.primary)
                        Spacer()
                        if store.config.fondoTipo == .satelite && store.config.sateliteNombre == lugar.nombre {
                            Image(systemName: "checkmark").foregroundColor(.accentColor)
                        }
                    }
                }
                .disabled(descargando)
            }
        } header: {
            Text("Imágenes satelitales")
        }
        Section {
            HStack {
                TextField("Latitud", text: $latTxt).keyboardType(.numbersAndPunctuation)
                TextField("Longitud", text: $lonTxt).keyboardType(.numbersAndPunctuation)
            }
            Stepper("Zoom: \(zoom)", value: $zoom, in: 3...18)
            Button("Descargar este lugar") {
                let lat = Double(latTxt.replacingOccurrences(of: ",", with: "."))
                let lon = Double(lonTxt.replacingOccurrences(of: ",", with: "."))
                if let lat, let lon, (-85...85).contains(lat), (-180...180).contains(lon) {
                    descargar("Mi lugar", lat, lon, zoom)
                } else {
                    mensaje = "Coordenadas no válidas."
                }
            }
            .disabled(descargando)
            if descargando { ProgressView() }
            if let mensaje { Text(mensaje).foregroundColor(.accentColor) }
        } header: {
            Text("Tus coordenadas")
        } footer: {
            Text("Imágenes: Esri, Maxar, Earthstar Geographics y la comunidad de usuarios GIS.")
        }
        Section("Ajustes de imagen") {
            FilaDeslizador(titulo: "Oscurecer", valor: $store.config.fondoOscurecer, rango: 0...0.8, formato: pct)
            FilaDeslizador(titulo: "Desenfoque", valor: $store.config.fondoDesenfoque, rango: 0...25) { "\(Int($0.rounded()))" }
        }
    }

    private func descargar(_ nombre: String, _ lat: Double, _ lon: Double, _ z: Int) {
        guard !descargando else { return }
        descargando = true
        mensaje = "Descargando \(nombre)…"
        Task {
            do {
                try await Fondos.descargarSatelite(lat: lat, lon: lon, zoom: z)
                store.config.fondoTipo = .satelite
                store.config.sateliteNombre = nombre
                store.config.fondoOculto = false
                store.config.fondoVersion += 1
                mensaje = "\(nombre) aplicado ✓"
            } catch {
                mensaje = "No se pudo descargar. Revisa tu conexión."
            }
            descargando = false
        }
    }
}

private struct SeccionTeclas: View {
    @ObservedObject var store: ConfigStore
    var body: some View {
        Section("Forma") {
            FilaDeslizador(titulo: "Redondeo", valor: $store.config.radioTecla, rango: 0...22, formato: dp)
            FilaDeslizador(titulo: "Espacio entre teclas", valor: $store.config.espaciado, rango: 2...14, formato: dp)
            FilaDeslizador(titulo: "Opacidad", valor: $store.config.opacidadTecla, rango: 0.2...1, formato: pct)
            FilaDeslizador(titulo: "Tamaño de letra", valor: $store.config.escalaTexto, rango: 0.8...1.3, formato: pct)
        }
        Section("Detalles") {
            Toggle("Sombra", isOn: $store.config.sombra)
            Toggle("Bordes", isOn: $store.config.bordes)
            Toggle("Burbuja al presionar", isOn: $store.config.popup)
        }
    }
}

private struct SeccionEscritura: View {
    @ObservedObject var store: ConfigStore
    var body: some View {
        Section {
            Toggle("Vibración", isOn: $store.config.vibracion)
            if store.config.vibracion {
                FilaDeslizador(titulo: "Fuerza", valor: $store.config.fuerzaVibracion, rango: 0.1...1, formato: pct)
            }
            Toggle("Sonido", isOn: $store.config.sonido)
        } header: {
            Text("Sensación")
        } footer: {
            Text("La vibración y el sonido necesitan «Permitir acceso total».")
        }
        Section("Ayudas") {
            Toggle("Mayúscula automática", isOn: $store.config.mayusAuto)
            Toggle("Punto con doble espacio", isOn: $store.config.puntoDobleEspacio)
            Toggle("Mover cursor deslizando el espacio", isOn: $store.config.cursorEnEspacio)
            Toggle("Acentos manteniendo la tecla", isOn: $store.config.acentosLargos)
        }
        Section {
            Button("Restablecer todo", role: .destructive) { store.restablecer() }
        }
    }
}
