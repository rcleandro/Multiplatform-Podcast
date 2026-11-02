import AVKit
import SwiftUI

/// The system's AirPlay picker (25.10), where the Compose player has its Chromecast button (18.17): it lists the
/// speakers and TVs nearby and sends the episode to the one chosen. A video episode puts the TVs first.
struct AirPlayButton: UIViewRepresentable {
    let prefersVideo: Bool

    func makeUIView(context: Context) -> AVRoutePickerView {
        let picker = AVRoutePickerView()
        // Like the bar's other buttons, and the theme's color while the audio goes to another device.
        picker.tintColor = .label
        picker.activeTintColor = UIColor(resource: .Theme.primary)
        return picker
    }

    func updateUIView(_ picker: AVRoutePickerView, context: Context) {
        picker.prioritizesVideoDevices = prefersVideo
    }
}
