// Run on macOS: swift core/ui/designsystem/design/export-launcher-icons.swift [preview.png]
// The SVG is the editable source; the design system owns all Android icon resources.
import AppKit
import Foundation

let moduleRoot = URL(fileURLWithPath: #filePath)
    .deletingLastPathComponent().deletingLastPathComponent()
let source = moduleRoot.appendingPathComponent("design/news-app-icon.svg")
guard let image = NSImage(contentsOf: source) else {
    fatalError("Cannot open \(source.path)")
}

enum IconShape { case source, rounded, circle }

func render(size: Int, shape: IconShape, destination: URL) throws {
    let bitmap = NSBitmapImageRep(
        bitmapDataPlanes: nil, pixelsWide: size, pixelsHigh: size,
        bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false,
        colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0
    )!
    let bounds = NSRect(x: 0, y: 0, width: size, height: size)
    NSGraphicsContext.saveGraphicsState()
    let context = NSGraphicsContext(bitmapImageRep: bitmap)!
    NSGraphicsContext.current = context
    context.imageInterpolation = .high
    context.cgContext.clear(bounds)
    if shape == .circle {
        NSBezierPath(ovalIn: bounds).addClip()
    } else if shape == .rounded {
        let radius = CGFloat(size) * 0.22
        NSBezierPath(roundedRect: bounds, xRadius: radius, yRadius: radius).addClip()
    }
    // Adaptive launchers mask the central 72 dp of a 108 dp asset. Apply the same
    // crop to legacy icons so their newspaper is the same visual size.
    let crop = shape == .source ? NSRect.zero : NSRect(x: 18, y: 18, width: 72, height: 72)
    image.draw(in: bounds, from: crop, operation: .sourceOver, fraction: 1)
    NSGraphicsContext.restoreGraphicsState()
    guard let data = bitmap.representation(using: .png, properties: [:]) else {
        fatalError("Could not encode launcher icon")
    }
    try data.write(to: destination, options: .atomic)
}

try render(size: 1024, shape: .source, destination: moduleRoot.appendingPathComponent("design/app-icon-source.png"))
for (density, size) in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)] {
    let directory = moduleRoot.appendingPathComponent("src/main/res/mipmap-\(density)")
    try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
    for (name, shape) in [("ic_launcher", IconShape.rounded), ("ic_launcher_round", IconShape.circle)] {
        try render(size: size, shape: shape, destination: directory.appendingPathComponent("\(name).png"))
        let previous = directory.appendingPathComponent("\(name).webp")
        if FileManager.default.fileExists(atPath: previous.path) {
            try FileManager.default.removeItem(at: previous)
        }
    }
}
if let preview = CommandLine.arguments.dropFirst().first {
    try render(size: 512, shape: .circle, destination: URL(fileURLWithPath: preview))
}
print("Exported launcher icons for all five Android densities")
