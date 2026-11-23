import UIKit
import ImageIO

struct LugarSatelital: Identifiable {
    let nombre: String
    let lat: Double
    let lon: Double
    let zoom: Int
    var id: String { nombre }
}

/// Fondos del teclado guardados en el App Group.
enum Fondos {
    static let lugares: [LugarSatelital] = [
        LugarSatelital(nombre: "Ciudad de México", lat: 19.4326, lon: -99.1332, zoom: 13),
        LugarSatelital(nombre: "Chichén Itzá", lat: 20.6829, lon: -88.5686, zoom: 17),
        LugarSatelital(nombre: "Gran Cañón", lat: 36.1069, lon: -112.1129, zoom: 12),
        LugarSatelital(nombre: "Monte Fuji", lat: 35.3606, lon: 138.7274, zoom: 12),
        LugarSatelital(nombre: "Everest", lat: 27.9881, lon: 86.9250, zoom: 12),
        LugarSatelital(nombre: "Sahara", lat: 23.4162, lon: 25.6628, zoom: 9),
        LugarSatelital(nombre: "Gran Barrera de Coral", lat: -18.2871, lon: 147.6992, zoom: 10),
        LugarSatelital(nombre: "Maldivas", lat: 4.1755, lon: 73.5093, zoom: 12),
        LugarSatelital(nombre: "Ámsterdam", lat: 52.3676, lon: 4.9041, zoom: 14),
        LugarSatelital(nombre: "Islandia", lat: 64.40, lon: -16.80, zoom: 9),
    ]

    static var galeria: URL { AppGroup.carpeta.appendingPathComponent("fondo_galeria.jpg") }
    static var satelite: URL { AppGroup.carpeta.appendingPathComponent("fondo_satelite.jpg") }

    /// Imagen reducida (el teclado tiene poca memoria disponible).
    static func cargar(_ tipo: BackgroundType) -> UIImage? {
        switch tipo {
        case .tema: return nil
        case .galeria: return reducir(url: galeria, max: 1200)
        case .satelite: return reducir(url: satelite, max: 1200)
        }
    }

    static func reducir(url: URL, max: Int) -> UIImage? {
        guard let src = CGImageSourceCreateWithURL(url as CFURL, nil) else { return nil }
        return miniatura(src, max)
    }

    static func reducir(data: Data, max: Int) -> UIImage? {
        guard let src = CGImageSourceCreateWithData(data as CFData, nil) else { return nil }
        return miniatura(src, max)
    }

    private static func miniatura(_ src: CGImageSource, _ max: Int) -> UIImage? {
        let opciones: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceThumbnailMaxPixelSize: max,
        ]
        guard let cg = CGImageSourceCreateThumbnailAtIndex(src, 0, opciones as CFDictionary) else { return nil }
        return UIImage(cgImage: cg)
    }

    static func guardarGaleria(_ data: Data) -> Bool {
        guard let img = reducir(data: data, max: 1600), let jpg = img.jpegData(compressionQuality: 0.9) else { return false }
        return (try? jpg.write(to: galeria, options: .atomic)) != nil
    }

    /// Mosaico de 4x2 teselas (1024x512) de Esri World Imagery centrado en lat/lon.
    static func descargarSatelite(lat: Double, lon: Double, zoom: Int) async throws {
        let z = min(Swift.max(zoom, 2), 18)
        let n = 1 << z
        let latR = min(Swift.max(lat, -85), 85) * .pi / 180
        let xf = (lon + 180) / 360 * Double(n)
        let yf = (1 - log(tan(latR) + 1 / cos(latR)) / .pi) / 2 * Double(n)
        let cols = 4, filas = 2
        let x0 = Int(floor(xf - Double(cols) / 2))
        let y0 = Int(floor(yf - Double(filas) / 2))

        var teselas: [(Int, Int, UIImage)] = []
        for r in 0..<filas {
            for c in 0..<cols {
                let x = ((x0 + c) % n + n) % n
                let y = min(Swift.max(y0 + r, 0), n - 1)
                guard let url = URL(string: "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/\(z)/\(y)/\(x)") else { continue }
                var req = URLRequest(url: url)
                req.timeoutInterval = 15
                req.setValue("DuoKeyboard/1.0 (iOS)", forHTTPHeaderField: "User-Agent")
                let (data, _) = try await URLSession.shared.data(for: req)
                guard let img = UIImage(data: data) else { throw URLError(.cannotDecodeContentData) }
                teselas.append((c, r, img))
            }
        }

        let formato = UIGraphicsImageRendererFormat()
        formato.scale = 1
        let render = UIGraphicsImageRenderer(size: CGSize(width: cols * 256, height: filas * 256), format: formato)
        let imagen = render.image { _ in
            for (c, r, t) in teselas { t.draw(in: CGRect(x: c * 256, y: r * 256, width: 256, height: 256)) }
        }
        guard let jpg = imagen.jpegData(compressionQuality: 0.92) else { throw URLError(.cannotCreateFile) }
        try jpg.write(to: satelite, options: .atomic)
    }
}
