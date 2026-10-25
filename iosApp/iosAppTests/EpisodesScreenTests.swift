import Shared
import XCTest
@testable import Podcast

/// The Episodes tab (25.6): date headers and what an empty list says.
final class EpisodesScreenTests: XCTestCase {
    private let now = Int64(Date().timeIntervalSince1970 * 1000)
    private let day: Int64 = 24 * 60 * 60 * 1000

    func testEpisodesGoUnderTheirDateAndKeepTheirPlaceInTheList() {
        let list = [episode("a", published: now - day / 2), episode("b", published: now - 3 * day), episode("c", published: now - 4 * day)]

        let grouped = sections(list, filter: .all)

        XCTAssertEqual([.last24Hours, .last7Days], grouped.map(\.group))
        XCTAssertEqual([1, 2], grouped[1].rows.map(\.index))
    }

    func testHistoryGroupsByWhenEachLastPlayed() {
        let old = episode("a", published: now - 90 * day, lastPlayed: now - day / 2)

        XCTAssertEqual([.last24Hours], sections([old], filter: .history).map(\.group))
        XCTAssertEqual([.older], sections([old], filter: .all).map(\.group))
    }

    func testANoMatchSearchSaysSoWhateverTheFilter() {
        XCTAssertEqual(Res.string.shared.search_empty_title.key, EpisodesEmpty.of(filter: .saved, searching: true).0.key)
        XCTAssertEqual(Res.string.shared.saved_empty_title.key, EpisodesEmpty.of(filter: .saved, searching: false).0.key)
    }

    private func episode(_ id: String, published: Int64, lastPlayed: Int64? = nil) -> Episode {
        Episode(id: id, podcastId: "p", podcastTitle: nil, title: id, description: nil, audioUrl: "https://ex.com/\(id).mp3",
                imageUrl: nil, duration: 60, publishDate: published, isPlayed: false, playbackPosition: 0, isDownloaded: false,
                fileSize: nil, localPath: nil, link: nil, explicit: false, isNew: false, season: nil, type: .full, isSaved: false,
                lastPlayedAt: lastPlayed.map { KotlinLong(value: $0) }, downloadedAt: nil, chaptersUrl: nil, transcriptUrl: nil,
                transcriptType: nil, updatedAt: 0, mediaType: nil)
    }
}
