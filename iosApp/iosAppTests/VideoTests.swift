import AVFoundation
import Shared
import XCTest
@testable import Podcast

/// A video episode's picture (25.8e, ADR 0010): drawn from the AVPlayer that plays the episodes, in a layer that
/// follows its view and lets go of the player in the background outside picture-in-picture.
final class VideoTests: XCTestCase {
    func testThePictureComesFromThePlayerThatPlaysTheEpisodes() {
        let view = PlayerLayerView()
        view.playerLayer.player = VideoSurface_iosKt.videoPlayer()

        XCTAssertTrue(view.layer is AVPlayerLayer)
        XCTAssertTrue(view.playerLayer.player === VideoSurface_iosKt.videoPlayer())
    }

    /// iOS pauses a player drawn in a layer in the background: there the layer lets go of it, so the sound goes on.
    func testInTheBackgroundTheLayerLetsGoOfThePlayerAndTakesItBackOnReturn() {
        let view = PlayerLayerView()
        let player = AVPlayer()
        view.playerLayer.player = player

        view.didEnterBackground()
        XCTAssertNil(view.playerLayer.player)

        view.willEnterForeground()
        XCTAssertTrue(view.playerLayer.player === player)
    }

    func testInPictureInPictureTheLayerKeepsThePlayer() {
        let view = PlayerLayerView()
        let player = AVPlayer()
        view.playerLayer.player = player
        view.isInPictureInPicture = true

        view.didEnterBackground()

        XCTAssertTrue(view.playerLayer.player === player)
    }
}
