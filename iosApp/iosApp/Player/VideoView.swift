import AVKit
import Shared
import SwiftUI

/// A video episode's picture (25.8e, ADR 0010), drawn from the same AVPlayer that plays its sound. Leaving the app while
/// it plays goes to picture-in-picture, as the Android player does.
struct VideoView: UIViewRepresentable {
    func makeUIView(context: Context) -> PlayerLayerView {
        let view = PlayerLayerView()
        view.playerLayer.player = VideoSurface_iosKt.videoPlayer()
        view.playerLayer.videoGravity = .resizeAspect
        let pictureInPicture = AVPictureInPictureController(playerLayer: view.playerLayer)
        pictureInPicture?.canStartPictureInPictureAutomaticallyFromInline = true
        pictureInPicture?.delegate = context.coordinator
        context.coordinator.pictureInPicture = pictureInPicture
        context.coordinator.view = view
        return view
    }

    func updateUIView(_ view: PlayerLayerView, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator() }

    /// Keeps the picture-in-picture controller alive as long as the view, and tells the view when it is in it.
    final class Coordinator: NSObject, AVPictureInPictureControllerDelegate {
        var pictureInPicture: AVPictureInPictureController?
        weak var view: PlayerLayerView?

        func pictureInPictureControllerWillStartPictureInPicture(_ controller: AVPictureInPictureController) {
            view?.isInPictureInPicture = true
        }

        func pictureInPictureControllerDidStopPictureInPicture(_ controller: AVPictureInPictureController) {
            view?.isInPictureInPicture = false
        }
    }
}

/// A view whose own layer is the `AVPlayerLayer`, so it follows the view's size. iOS pauses a player drawn in a layer
/// when the app goes to the background, so outside picture-in-picture the layer lets go of it there and the episode
/// goes on as sound, as an audio one does; it takes it back on return.
final class PlayerLayerView: UIView {
    override static var layerClass: AnyClass { AVPlayerLayer.self }

    var playerLayer: AVPlayerLayer { layer as! AVPlayerLayer }

    /// The player let go of in the background, to take back on return.
    private var detached: AVPlayer?

    override init(frame: CGRect) {
        super.init(frame: frame)
        let center = NotificationCenter.default
        center.addObserver(self, selector: #selector(didEnterBackground), name: UIApplication.didEnterBackgroundNotification, object: nil)
        center.addObserver(self, selector: #selector(willEnterForeground), name: UIApplication.willEnterForegroundNotification, object: nil)
    }

    required init?(coder: NSCoder) { nil }

    @objc func didEnterBackground() {
        guard !isInPictureInPicture else { return }
        detached = playerLayer.player
        playerLayer.player = nil
    }

    @objc func willEnterForeground() {
        if let detached { playerLayer.player = detached }
        detached = nil
    }

    /// From when picture-in-picture is about to start until it stops; in it, the layer must keep the player.
    var isInPictureInPicture = false
}
