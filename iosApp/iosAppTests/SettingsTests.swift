import Shared
import XCTest
@testable import Podcast

/// Settings and listening stats (25.8b): what each row says for each value, in Portuguese whatever the simulator's.
final class SettingsTests: XCTestCase {
    private let pt = Bundle(path: Bundle.main.path(forResource: "pt", ofType: "lproj")!)!
    private let idle = Res.string.shared.settings_import_opml_description

    func testAnImportRowSaysHowItsLastRunWent() {
        XCTAssertEqual("Importando 3 de 10…", SettingsTexts.importStatus(OpmlStatusImporting(done: 3, total: 10), idle: idle, bundle: pt))
        XCTAssertEqual("Adicionados: 2 · Já na biblioteca: 1 · Sem leitura: 0",
                       SettingsTexts.importStatus(OpmlStatusImported(result: OpmlImport(added: 2, known: 1, failed: 0)), idle: idle, bundle: pt))
        XCTAssertEqual("O arquivo não tem podcasts para importar",
                       SettingsTexts.importStatus(OpmlStatusImported(result: OpmlImport(added: 0, known: 0, failed: 0)), idle: idle, bundle: pt))
        XCTAssertEqual(idle.localized(bundle: pt), SettingsTexts.importStatus(OpmlStatusIdle.shared, idle: idle, bundle: pt))
        XCTAssertEqual("Salvo com 12 podcasts", SettingsTexts.exportStatus(OpmlStatusExported(podcasts: 12), idle: idle, bundle: pt))
    }

    func testDownloadChoicesReadAsTheComposeOnes() {
        XCTAssertEqual("Manter", SettingsTexts.deleteAfter(0, bundle: pt))
        XCTAssertEqual("30 dias", SettingsTexts.deleteAfter(30, bundle: pt))
        XCTAssertEqual("Sem limite", SettingsTexts.limit(nil, bundle: pt))
    }

    func testRefreshChoicesReadAsTheComposeOnes() {
        XCTAssertEqual("Nunca, só ao abrir o app", SettingsTexts.refresh(0, bundle: pt))
        XCTAssertEqual("A cada hora", SettingsTexts.refresh(1, bundle: pt))
        XCTAssertEqual("A cada 6 horas", SettingsTexts.refresh(6, bundle: pt))
    }

    /// The new-episodes notification (25.11) says what the Android one says (18.18).
    func testTheNewEpisodesNotificationNamesTheEpisodeOrCountsThem() {
        let one = BackgroundRefresh.message([episode("a", podcast: "Show")], bundle: pt)
        XCTAssertEqual("Show", one.title)
        XCTAssertEqual("a", one.body)
        let many = BackgroundRefresh.message([episode("a", podcast: "Show"), episode("b", podcast: "Other"),
                                              episode("c", podcast: "Show")], bundle: pt)
        XCTAssertEqual("3 episódios novos", many.title)
        XCTAssertEqual("Show, Other", many.body)
    }

    func testStatsWriteTimeAsEpisodeLengthsAndWeeksByTheirMonday() {
        XCTAssertEqual("1h 5min", StatsTexts.heard(65 * 60 * 1000))
        // 2026-10-19, a Monday, is day 20 745 since 1970-01-01.
        XCTAssertEqual("19 de out.", StatsTexts.weekLabel(20_745, locale: Locale(identifier: "pt_BR")))
    }

    private func episode(_ id: String, podcast: String) -> Episode {
        Episode(id: id, podcastId: podcast, podcastTitle: podcast, title: id, description: nil, audioUrl: "https://ex.com/\(id).mp3",
                imageUrl: nil, duration: 60, publishDate: 0, isPlayed: false, playbackPosition: 0, isDownloaded: false,
                fileSize: nil, localPath: nil, link: nil, explicit: false, isNew: true, season: nil, type: .full, isSaved: false,
                lastPlayedAt: nil, downloadedAt: nil, chaptersUrl: nil, transcriptUrl: nil, transcriptType: nil, updatedAt: 0,
                mediaType: nil)
    }
}
