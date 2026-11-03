import SwiftUI

/// A podcast's or episode's cover, square with the theme's corners, and a symbol while it loads or when there is none.
struct Artwork: View {
    let url: String?
    let size: CGFloat?
    var radius = Radius.medium

    var body: some View {
        AsyncImage(url: url.flatMap(URL.init(string:))) { image in
            image.resizable().scaledToFill()
        } placeholder: {
            Image(systemName: "mic").font(.title2).foregroundStyle(Color(.Theme.onSurfaceVariant))
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color(.Theme.surfaceVariant))
        }
        .frame(width: size, height: size)
        .aspectRatio(1, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: radius))
        .accessibilityHidden(true)
    }
}
