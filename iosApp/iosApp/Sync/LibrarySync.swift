import Foundation
import WatchConnectivity
#if os(watchOS)
import WatchShared
#else
import Shared
#endif

/**
 * iPhone ↔ Apple Watch library sync (25.14d, ADR 0016), as the Wear OS one (19.9b): each side sends its snapshot as a
 * file once its changes settle, and merges the other's when it arrives. WatchConnectivity delivers a file even with the
 * other app closed, and encrypts it, so private feed addresses may travel. The same file serves both apps.
 */
final class LibrarySync: NSObject, WCSessionDelegate {
    static let shared = LibrarySync()
    private var watching: Watching?

    func start() {
        guard WCSession.isSupported() else { return }
        WCSession.default.delegate = self
        WCSession.default.activate()
    }

    func session(_ session: WCSession, activationDidCompleteWith state: WCSessionActivationState, error: Error?) {
        guard state == .activated else { return }
        DispatchQueue.main.async { self.follow() }
    }

    private func follow() {
        guard watching == nil else { return }
        watching = LibrarySyncBridgeKt.followLibrary { [weak self] text in self?.send(text) }
    }

    private func send(_ text: String) {
        let session = WCSession.default
        #if os(iOS)
        guard session.isPaired, session.isWatchAppInstalled else { return }
        #endif
        // Only the newest snapshot matters: an older one still on its way would only be merged and replaced.
        session.outstandingFileTransfers.forEach { $0.cancel() }
        let file = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString + ".json")
        guard (try? Data(text.utf8).write(to: file)) != nil else { return }
        session.transferFile(file, metadata: nil)
    }

    func session(_ session: WCSession, didFinish fileTransfer: WCSessionFileTransfer, error: Error?) {
        try? FileManager.default.removeItem(at: fileTransfer.file.fileURL)
    }

    func session(_ session: WCSession, didReceive file: WCSessionFile) {
        // The file is gone once this returns: read it first.
        guard let text = try? String(contentsOf: file.fileURL, encoding: .utf8) else { return }
        Task { @MainActor in try? await LibrarySyncBridgeKt.receiveLibrary(text: text) }
    }

    #if os(iOS)
    func sessionDidBecomeInactive(_ session: WCSession) {}

    /// The person switched to another watch: talk to the new one.
    func sessionDidDeactivate(_ session: WCSession) {
        session.activate()
    }
    #endif
}
