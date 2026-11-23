import SwiftUI

/// Tamaños calculados del teclado (los usa la vista y el controlador para su altura).
struct Medidas {
    let modo: LayoutMode
    let filaAlto: CGFloat
    let barra: CGFloat
    let util: CGFloat
    let hueco: CGFloat
    let mitad: CGFloat
    let extra: CGFloat
    let total: CGFloat

    init(config c: KeyboardConfig, page: Page, horizontal: Bool, ancho: CGFloat, conBarra: Bool) {
        modo = horizontal && c.duoEnHorizontal ? .duo : c.modoTeclado
        filaAlto = 46 * CGFloat(c.alturaEscala) * (horizontal ? 0.78 : 1)
        barra = conBarra ? 44 : 6
        util = max(ancho - 6, 0)
        if modo == .duo {
            hueco = util * CGFloat(c.separacionDuo)
            mitad = (util - hueco) / 2
            extra = mitad * CGFloat(sin(c.anguloDuo * .pi / 180)) + CGFloat(c.curva)
        } else {
            hueco = 0
            mitad = util
            extra = CGFloat(c.curva)
        }
        let filas = CGFloat(Layouts.filas(page, numeros: c.filaNumeros).count)
        total = barra + filaAlto * filas + extra + 6 + CGFloat(c.elevacion)
    }
}

struct KeyboardView: View {
    @ObservedObject var state: KeyboardState
    @ObservedObject var store: ConfigStore
    var fondo: UIImage?
    var horizontal: Bool
    var ancho: CGFloat
    var esVistaPrevia = false

    var body: some View {
        let c = store.config
        let t = Tema.para(c)
        let m = Medidas(config: c, page: state.page, horizontal: horizontal, ancho: ancho,
                        conBarra: c.barraHerramientas || !state.acentos.isEmpty)
        ZStack(alignment: .top) {
            LinearGradient(colors: t.fondo, startPoint: .top, endPoint: .bottom)
            if c.fondoTipo != .tema, !c.fondoOculto, let img = fondo {
                Image(uiImage: img)
                    .resizable()
                    .scaledToFill()
                    .frame(width: ancho, height: m.total)
                    .blur(radius: CGFloat(c.fondoDesenfoque))
                    .clipped()
                Color.black.opacity(c.fondoOscurecer)
            }
            VStack(spacing: 0) {
                franja(c, t, m)
                teclas(c, t, m).padding(.horizontal, 3)
                Spacer(minLength: 0)
            }
        }
        .frame(width: ancho, height: m.total)
        .clipped()
    }

    // MARK: Barra superior

    @ViewBuilder
    private func franja(_ c: KeyboardConfig, _ t: Tema, _ m: Medidas) -> some View {
        if !state.acentos.isEmpty {
            HStack(spacing: 6) {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(state.acentos, id: \.self) { s in
                            Button { state.elegirAcento(s) } label: {
                                Text(s)
                                    .font(.system(size: 20))
                                    .foregroundColor(t.textoBurbuja)
                                    .frame(width: 42, height: 34)
                                    .background(RoundedRectangle(cornerRadius: 8, style: .continuous).fill(t.burbuja))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 6)
                }
                boton("xmark", t) { state.cerrarAcentos() }
            }
            .frame(height: m.barra)
        } else if c.barraHerramientas {
            HStack(spacing: 2) {
                boton(c.modoTeclado.simbolo, t) { store.config.modoTeclado = store.config.modoTeclado.siguiente }
                boton("paintpalette", t) { store.config.tema = store.config.tema.siguiente }
                if c.fondoTipo != .tema {
                    boton(c.fondoOculto ? "eye.slash" : "photo", t) { store.config.fondoOculto.toggle() }
                }
                Spacer()
                Text("duo")
                    .font(.system(size: 13, weight: .semibold))
                    .kerning(2)
                    .foregroundColor(t.acento)
                Spacer()
                if !esVistaPrevia && state.muestraGlobo {
                    boton("globe", t) { state.alCambiarTeclado() }
                }
                if !esVistaPrevia {
                    boton("keyboard.chevron.compact.down", t) { state.alOcultar() }
                }
            }
            .padding(.horizontal, 8)
            .frame(height: m.barra)
        } else {
            Color.clear.frame(height: m.barra)
        }
    }

    private func boton(_ simbolo: String, _ t: Tema, _ accion: @escaping () -> Void) -> some View {
        Button(action: accion) {
            Image(systemName: simbolo)
                .font(.system(size: 17, weight: .regular))
                .foregroundColor(t.iconos)
                .frame(width: 38, height: 38)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    // MARK: Teclas

    @ViewBuilder
    private func teclas(_ c: KeyboardConfig, _ t: Tema, _ m: Medidas) -> some View {
        let filas = Layouts.filas(state.page, numeros: c.filaNumeros)
        let curva = CGFloat(c.curva)
        let arco: (CGFloat) -> CGFloat = { x in curva * (1 - (2 * x - 1) * (2 * x - 1)) }
        switch m.modo {
        case .completo:
            bloque(filas.map(Layouts.unir), duo: false, ancho: m.util, c, t, m, curva: arco)
        case .duo:
            HStack(alignment: .top, spacing: m.hueco) {
                bloque(filas.map { $0.left }, duo: true, ancho: m.mitad, c, t, m, curva: { x in curva * x * x })
                    .rotationEffect(.degrees(c.anguloDuo), anchor: .bottomLeading)
                bloque(filas.map { $0.right }, duo: true, ancho: m.mitad, c, t, m, curva: { x in curva * (1 - x) * (1 - x) })
                    .rotationEffect(.degrees(-c.anguloDuo), anchor: .bottomTrailing)
            }
        case .manoIzq, .manoDer:
            let w = m.util * CGFloat(c.anchoUnaMano)
            HStack(spacing: 0) {
                if m.modo == .manoDer { controlesUnaMano(haciaIzquierda: true, t).frame(width: m.util - w) }
                bloque(filas.map(Layouts.unir), duo: false, ancho: w, c, t, m, curva: arco)
                if m.modo == .manoIzq { controlesUnaMano(haciaIzquierda: false, t).frame(width: m.util - w) }
            }
        }
    }

    private func controlesUnaMano(haciaIzquierda: Bool, _ t: Tema) -> some View {
        VStack(spacing: 10) {
            boton(haciaIzquierda ? "chevron.left" : "chevron.right", t) {
                store.config.modoTeclado = haciaIzquierda ? .manoIzq : .manoDer
            }
            boton(LayoutMode.duo.simbolo, t) { store.config.modoTeclado = .duo }
            boton(LayoutMode.completo.simbolo, t) { store.config.modoTeclado = .completo }
        }
    }

    private func bloque(_ filas: [[Key]], duo: Bool, ancho: CGFloat, _ c: KeyboardConfig, _ t: Tema, _ m: Medidas,
                        curva: @escaping (CGFloat) -> CGFloat) -> some View {
        VStack(spacing: 0) {
            ForEach(Array(filas.enumerated()), id: \.offset) { _, fila in
                filaView(fila, duo: duo, ancho: ancho, c, t, m, curva: curva)
            }
        }
        .frame(width: ancho)
    }

    private func filaView(_ fila: [Key], duo: Bool, ancho: CGFloat, _ c: KeyboardConfig, _ t: Tema, _ m: Medidas,
                          curva: @escaping (CGFloat) -> CGFloat) -> some View {
        let total = fila.reduce(CGFloat(0)) { $0 + $1.peso(duo: duo) }
        var inicios: [CGFloat] = []
        var acumulado: CGFloat = 0
        for k in fila {
            inicios.append(acumulado)
            acumulado += k.peso(duo: duo)
        }
        return HStack(spacing: 0) {
            ForEach(Array(fila.enumerated()), id: \.offset) { i, key in
                let peso = key.peso(duo: duo)
                let x = (inicios[i] + peso / 2) / total
                TeclaView(key: key, state: state, config: c, tema: t, alto: m.filaAlto)
                    .frame(width: ancho * peso / total, height: m.filaAlto)
                    .offset(y: curva(x))
            }
        }
    }
}

// MARK: - Una tecla

struct TeclaView: View {
    let key: Key
    @ObservedObject var state: KeyboardState
    let config: KeyboardConfig
    let tema: Tema
    let alto: CGFloat

    @State private var presionada = false
    @State private var mantenido = false
    @State private var modoCursor = false
    @State private var acumulado: CGFloat = 0
    @State private var ultimoX: CGFloat = 0
    @State private var tarea: Task<Void, Never>?

    private var especial: Bool { key.type != .char && key.type != .space }
    private var enterAccion: Bool { key.type == .enter && state.etiquetaEnter != nil }
    private var shiftActivo: Bool { key.type == .shift && state.shift != .off }

    private var colorBase: Color {
        if enterAccion { return tema.acento }
        if shiftActivo { return tema.activa }
        return especial ? tema.especial : tema.tecla
    }

    private var colorTexto: Color {
        if enterAccion { return tema.textoAcento }
        if shiftActivo { return tema.textoActiva }
        return especial ? tema.textoEspecial : tema.texto
    }

    private var opacidadRelleno: Double {
        let resaltada = presionada || enterAccion || shiftActivo
        switch config.estiloTecla {
        case .solida: return config.opacidadTecla
        case .contorno: return config.opacidadTecla * (resaltada ? 0.95 : 0.3)
        case .minimal: return resaltada ? config.opacidadTecla : 0
        }
    }

    var body: some View {
        let forma = RoundedRectangle(cornerRadius: CGFloat(config.radioTecla), style: .continuous)
        ZStack {
            if config.sombra && config.estiloTecla == .solida {
                forma.fill(tema.sombra.opacity(config.opacidadTecla)).offset(y: 1.2)
            }
            forma.fill((presionada ? tema.presionada : colorBase).opacity(opacidadRelleno))
            if config.bordes || config.estiloTecla == .contorno {
                forma.stroke(config.estiloTecla == .contorno ? colorTexto.opacity(0.45) : tema.borde, lineWidth: 1)
            }
            contenido
        }
        .padding(.horizontal, CGFloat(config.espaciado) / 2)
        .padding(.vertical, CGFloat(config.espaciado) / 2 + 1)
        .contentShape(Rectangle())
        .overlay(alignment: .top) {
            if presionada && key.type == .char && config.popup && !mantenido {
                burbuja
            }
        }
        .zIndex(presionada ? 10 : 0)
        .gesture(
            DragGesture(minimumDistance: 0, coordinateSpace: .local)
                .onChanged(alMover)
                .onEnded { _ in alSoltar() }
        )
    }

    @ViewBuilder
    private var contenido: some View {
        let fs = alto * 0.5 * CGFloat(config.escalaTexto)
        switch key.type {
        case .shift:
            Image(systemName: state.shift == .lock ? "capslock.fill" : (state.shift == .once ? "shift.fill" : "shift"))
                .font(.system(size: fs * 0.8, weight: .medium))
                .foregroundColor(colorTexto)
        case .delete:
            Image(systemName: "delete.left")
                .font(.system(size: fs * 0.8, weight: .medium))
                .foregroundColor(colorTexto)
        case .enter:
            if let e = state.etiquetaEnter {
                Text(e).font(.system(size: fs * 0.6, weight: .medium)).foregroundColor(colorTexto)
                    .lineLimit(1).minimumScaleFactor(0.5)
            } else {
                Image(systemName: "return").font(.system(size: fs * 0.8, weight: .medium)).foregroundColor(colorTexto)
            }
        case .space:
            Text("espacio").font(.system(size: fs * 0.55)).foregroundColor(colorTexto.opacity(0.75)).lineLimit(1)
        case .char:
            let tx = state.textoTecla(key)
            Text(tx)
                .font(.system(size: tx.first?.isLetter == true ? fs : fs * 0.85))
                .foregroundColor(colorTexto)
                .lineLimit(1).minimumScaleFactor(0.5)
        default:
            Text(key.label).font(.system(size: fs * 0.6, weight: .medium)).foregroundColor(colorTexto)
                .lineLimit(1).minimumScaleFactor(0.5)
        }
    }

    private var burbuja: some View {
        Text(state.textoTecla(key))
            .font(.system(size: alto * 0.62))
            .foregroundColor(tema.textoBurbuja)
            .frame(minWidth: 44)
            .frame(height: alto * 1.05)
            .padding(.horizontal, 6)
            .background(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(tema.burbuja)
                    .shadow(color: .black.opacity(0.3), radius: 4, y: 2)
            )
            .offset(y: -alto * 1.05 - 2)
            .allowsHitTesting(false)
    }

    // MARK: Gestos: tocar, mantener, repetir (borrar) y deslizar el espacio

    private func alMover(_ v: DragGesture.Value) {
        if !presionada {
            presionada = true
            mantenido = false
            modoCursor = false
            acumulado = 0
            ultimoX = v.location.x
            state.alPresionar(key)
            iniciarTarea()
            return
        }
        guard key.type == .space, config.cursorEnEspacio else { return }
        let total = v.translation.width
        if !modoCursor && abs(total) > 10 {
            modoCursor = true
            acumulado = total
            tarea?.cancel()
        } else if modoCursor {
            acumulado += v.location.x - ultimoX
        }
        if modoCursor {
            let pasos = Int(acumulado / 9)
            if pasos != 0 {
                state.moverCursor(pasos)
                acumulado -= CGFloat(pasos) * 9
            }
        }
        ultimoX = v.location.x
    }

    private func alSoltar() {
        tarea?.cancel()
        tarea = nil
        if !mantenido && !modoCursor { state.alTocar(key) }
        presionada = false
        mantenido = false
        modoCursor = false
    }

    private func iniciarTarea() {
        tarea?.cancel()
        let k = key
        tarea = Task { @MainActor in
            if k.type == .delete {
                try? await Task.sleep(nanoseconds: 380_000_000)
                var intervalo: UInt64 = 70_000_000
                while !Task.isCancelled && presionada {
                    state.alRepetir(k)
                    try? await Task.sleep(nanoseconds: intervalo)
                    intervalo = max(30_000_000, intervalo - 5_000_000)
                }
            } else {
                try? await Task.sleep(nanoseconds: 420_000_000)
                if !Task.isCancelled && presionada && !modoCursor {
                    mantenido = state.alMantener(k)
                }
            }
        }
    }
}
