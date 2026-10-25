import Shared
import XCTest
@testable import Podcast

/// The player (25.7): what the sleep timer button says, and the choices it offers.
final class PlayerScreenTests: XCTestCase {
    func testTheTimerSaysWhatIsSet() {
        XCTAssertEqual(Res.string.shared.sleep_timer_short.localized(), SleepTimers.label(timer: nil, millisLeft: nil))
        XCTAssertEqual("14:05", SleepTimers.label(timer: SleepTimerMinutes(minutes: 15), millisLeft: 845_000))
        XCTAssertEqual(Res.string.shared.timer_end_of_episode.localized(),
                       SleepTimers.label(timer: SleepTimerEndOfEpisode.shared, millisLeft: nil))
    }

    func testTheTimerOffersOffFiveMinutesUpToAnHourAndTheEpisodesEnd() {
        let options = SleepTimers.options
        XCTAssertNil(options.first?.timer)
        XCTAssertEqual([5, 15, 30, 45, 60], options.compactMap { ($0.timer as? SleepTimerMinutes)?.minutes })
        XCTAssertTrue(options.last?.timer is SleepTimerEndOfEpisode)
    }
}
