import SwiftUI
import UIKit

/// A color as red, green and blue from 0 to 1, so the cover's color can be worked out and tested away from UIKit.
struct RGB: Equatable {
    var red: Double, green: Double, blue: Double

    init(red: Double, green: Double, blue: Double) {
        self.red = red
        self.green = green
        self.blue = blue
    }

    init(_ color: UIColor) {
        var red: CGFloat = 0, green: CGFloat = 0, blue: CGFloat = 0, alpha: CGFloat = 0
        color.getRed(&red, green: &green, blue: &blue, alpha: &alpha)
        self.init(red: red, green: green, blue: blue)
    }

    var color: Color { Color(red: red, green: green, blue: blue) }

    func mixed(with other: RGB, _ fraction: Double) -> RGB {
        RGB(red: red + (other.red - red) * fraction, green: green + (other.green - green) * fraction,
            blue: blue + (other.blue - blue) * fraction)
    }

    /// The WCAG relative luminance.
    var luminance: Double {
        func linear(_ channel: Double) -> Double {
            channel <= 0.04045 ? channel / 12.92 : pow((channel + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * linear(red) + 0.7152 * linear(green) + 0.0722 * linear(blue)
    }

    func contrast(with other: RGB) -> Double {
        let (a, b) = (luminance, other.luminance)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }
}

/// The cover's color, worked out as the Compose ArtworkColor.kt does (24.2).
enum ArtworkColor {
    static let sampleSize = 32
    private static let minSaturation = 0.25
    private static let minValue = 0.2
    private static let minColorfulShare = 0.05
    private static let tints = [0.35, 0.2]
    private static let textContrast = 4.5

    /// The average of the pixels that carry color (not greys, whites or near black), or nil when too few do, for
    /// example a black-and-white cover.
    static func dominant(_ pixels: [RGB]) -> RGB? {
        let colorful = pixels.filter { pixel in
            let high = max(pixel.red, pixel.green, pixel.blue)
            let low = min(pixel.red, pixel.green, pixel.blue)
            return high >= minValue && (high - low) / high >= minSaturation
        }
        guard !pixels.isEmpty, !colorful.isEmpty, Double(colorful.count) >= Double(pixels.count) * minColorfulShare else { return nil }
        let count = Double(colorful.count)
        return RGB(red: colorful.map(\.red).reduce(0, +) / count, green: colorful.map(\.green).reduce(0, +) / count,
                   blue: colorful.map(\.blue).reduce(0, +) / count)
    }

    /// The cover color blended into [background], as strong as the [foreground] text allows while keeping WCAG AA;
    /// nil when even a soft tint breaks the contrast, so the plain background stays.
    static func tint(_ dominant: RGB, background: RGB, foreground: RGB) -> RGB? {
        tints.lazy.map { background.mixed(with: dominant, $0) }.first { foreground.contrast(with: $0) >= textContrast }
    }

    /// The cover at [url], shrunk to a few pixels; the same URLSession as AsyncImage, so it usually comes from its cache.
    static func load(_ url: URL) async -> RGB? {
        guard let (data, _) = try? await URLSession.shared.data(from: url), let image = UIImage(data: data)?.cgImage else { return nil }
        let side = sampleSize
        var bytes = [UInt8](repeating: 0, count: side * side * 4)
        let drawn = bytes.withUnsafeMutableBytes { buffer -> Bool in
            guard let context = CGContext(data: buffer.baseAddress, width: side, height: side, bitsPerComponent: 8,
                                          bytesPerRow: side * 4, space: CGColorSpaceCreateDeviceRGB(),
                                          bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue) else { return false }
            context.draw(image, in: CGRect(x: 0, y: 0, width: side, height: side))
            return true
        }
        guard drawn else { return nil }
        let pixels = stride(from: 0, to: bytes.count, by: 4).map {
            RGB(red: Double(bytes[$0]) / 255, green: Double(bytes[$0 + 1]) / 255, blue: Double(bytes[$0 + 2]) / 255)
        }
        return dominant(pixels)
    }
}

/// A background tinted by the cover at [url]: a gradient over the whole screen, behind the bars, from its color at the
/// top to the plain background, as the Compose ArtworkBackdrop. A list over it hides its own background. The plain background stays when the cover has no clear color or the
/// tint would make the text unreadable.
private struct ArtworkBackdrop: ViewModifier {
    let url: String?
    @State private var dominant: RGB?
    @Environment(\.colorScheme) private var scheme

    func body(content: Content) -> some View {
        content.scrollContentBackground(.hidden).background {
            LinearGradient(colors: [top, Color(.Theme.background)], startPoint: .top, endPoint: .bottom)
                .ignoresSafeArea()
                .animation(.easeInOut, value: dominant)
        }
        .task(id: url) {
            guard let url = url.flatMap(URL.init(string:)) else { dominant = nil; return }
            dominant = await ArtworkColor.load(url)
        }
    }

    private var top: Color {
        let traits = UITraitCollection(userInterfaceStyle: scheme == .dark ? .dark : .light)
        let background = RGB(UIColor(resource: .Theme.background).resolvedColor(with: traits))
        let foreground = RGB(UIColor(resource: .Theme.onSurfaceVariant).resolvedColor(with: traits))
        return dominant.flatMap { ArtworkColor.tint($0, background: background, foreground: foreground) }?.color
            ?? Color(.Theme.background)
    }
}

extension View {
    func artworkBackdrop(url: String?) -> some View { modifier(ArtworkBackdrop(url: url)) }
}
