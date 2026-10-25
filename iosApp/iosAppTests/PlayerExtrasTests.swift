import Shared
import XCTest
@testable import Podcast

/// The player's extras (25.8e): sharing from the moment and the chapter names.
final class PlayerExtrasTests: XCTestCase {
    private let pt = Bundle(path: Bundle.main.path(forResource: "pt", ofType: "lproj")!)!

    func testSharingFromThePlayerSaysTheMomentAndStartsTheAudioThere() {
        let text = momentShareText(episode(), at: 754_000, bundle: pt)

        XCTAssertEqual("Ep · Show\nA partir de 12:34\nhttps://ex.com/a.mp3#t=754", text)
    }

    func testAChapterWithNoTitleIsNamedByWhenItStarts() {
        XCTAssertEqual("1:05", chapterLabel(Chapter(startMs: 65_000, title: " ")))
        XCTAssertEqual("Intro", chapterLabel(Chapter(startMs: 0, title: "Intro")))
    }

    private func episode() -> Episode {
        Episode(id: "e", podcastId: "https://ex.com/feed", podcastTitle: "Show", title: "Ep", description: nil,
                audioUrl: "https://ex.com/a.mp3", imageUrl: nil, duration: 60, publishDate: 0, isPlayed: false,
                playbackPosition: 0, isDownloaded: false, fileSize: nil, localPath: nil, link: nil, explicit: false,
                isNew: false, season: nil, type: .full, isSaved: false, lastPlayedAt: nil, downloadedAt: nil,
                chaptersUrl: nil, transcriptUrl: nil, transcriptType: nil, updatedAt: 0, mediaType: nil)
    }
}
