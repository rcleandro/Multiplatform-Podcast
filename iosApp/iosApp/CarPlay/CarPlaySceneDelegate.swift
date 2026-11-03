import CarPlay
import Shared

/**
 * CarPlay (25.13), as Android Auto (13.5): a tab with the library (a podcast, then its episodes) and one with the queue.
 * Picking an episode plays it and opens the system's Now Playing, which follows the player's MPNowPlayingInfoCenter and
 * remote commands, like the lock screen; its "Up Next" button opens the queue.
 */
final class CarPlaySceneDelegate: UIResponder, CPTemplateApplicationSceneDelegate, CPTabBarTemplateDelegate,
    CPNowPlayingTemplateObserver {
    private var interface: CPInterfaceController?
    private let catalog = CarCatalog_iosKt.carCatalog()
    private let library = CPListTemplate(title: Res.string.shared.library_title.localized(), sections: [])
    private let queue = CPListTemplate(title: Res.string.shared.queue_short.localized(), sections: [])

    func templateApplicationScene(_ scene: CPTemplateApplicationScene, didConnect interfaceController: CPInterfaceController) {
        interface = interfaceController
        library.tabImage = UIImage(systemName: "play.square.stack")
        library.emptyViewTitleVariants = [Res.string.shared.no_podcasts_found.localized()]
        queue.tabImage = UIImage(systemName: "list.bullet")
        queue.emptyViewTitleVariants = [Res.string.shared.queue_empty_title.localized()]
        let tabs = CPTabBarTemplate(templates: [library, queue])
        tabs.delegate = self
        interfaceController.setRootTemplate(tabs, animated: false, completion: nil)
        CPNowPlayingTemplate.shared.isUpNextButtonEnabled = true
        CPNowPlayingTemplate.shared.add(self)
        Task { await showLibrary() }
    }

    func templateApplicationScene(_ scene: CPTemplateApplicationScene,
                                  didDisconnectInterfaceController interfaceController: CPInterfaceController) {
        CPNowPlayingTemplate.shared.remove(self)
        interface = nil
    }

    /// Each tab is read again when chosen: the library or the queue may have changed on the phone.
    func tabBarTemplate(_ tabBarTemplate: CPTabBarTemplate, didSelect selectedTemplate: CPTemplate) {
        if selectedTemplate === library { Task { await showLibrary() } } else { showQueue() }
    }

    func nowPlayingTemplateUpNextButtonTapped(_ nowPlayingTemplate: CPNowPlayingTemplate) {
        showQueue()
        interface?.pushTemplate(queueCopy(), animated: true, completion: nil)
    }

    // MARK: Lists

    @MainActor
    private func showLibrary() async {
        guard let podcasts = try? await catalog.podcasts() else { return }
        let items = CarLists.podcastItems(podcasts) { [weak self] podcast in
            Task { await self?.open(podcast) }
        }
        library.updateSections([CPListSection(items: items)])
    }

    @MainActor
    private func open(_ podcast: Podcast) async {
        guard let episodes = try? await catalog.episodes(podcastId: podcast.id) else { return }
        let list = CPListTemplate(title: podcast.title, sections: [CPListSection(items: episodeItems(episodes))])
        interface?.pushTemplate(list, animated: true, completion: nil)
    }

    private func showQueue() {
        queue.updateSections([CPListSection(items: episodeItems(catalog.queue()))])
    }

    /// The queue again, pushed over Now Playing: a template shown in a tab cannot be pushed too.
    private func queueCopy() -> CPListTemplate {
        let copy = CPListTemplate(title: queue.title, sections: [CPListSection(items: episodeItems(catalog.queue()))])
        copy.emptyViewTitleVariants = queue.emptyViewTitleVariants
        return copy
    }

    private func episodeItems(_ episodes: [Episode]) -> [CPListItem] {
        CarLists.episodeItems(episodes) { [weak self] episode, done in
            Task { @MainActor in
                try? await self?.catalog.play(episodeId: episode.id)
                self?.interface?.pushTemplate(CPNowPlayingTemplate.shared, animated: true, completion: nil)
                done()
            }
        }
    }
}

/// The rows of the CarPlay lists, as many as the car takes, with covers that load after the row shows.
enum CarLists {
    static func podcastItems(_ podcasts: [Podcast], onSelect: @escaping (Podcast) -> Void) -> [CPListItem] {
        podcasts.prefix(CPListTemplate.maximumItemCount).map { podcast in
            let item = CPListItem(text: podcast.title, detailText: podcast.author)
            item.accessoryType = .disclosureIndicator
            item.handler = { _, done in
                onSelect(podcast)
                done()
            }
            loadCover(podcast.imageUrl, into: item)
            return item
        }
    }

    static func episodeItems(_ episodes: [Episode], onSelect: @escaping (Episode, @escaping () -> Void) -> Void) -> [CPListItem] {
        episodes.prefix(CPListTemplate.maximumItemCount).map { episode in
            let item = CPListItem(text: episode.title, detailText: episode.podcastTitle)
            item.handler = { _, done in onSelect(episode, done) }
            loadCover(episode.imageUrl, into: item)
            return item
        }
    }

    private static func loadCover(_ url: String?, into item: CPListItem) {
        guard let url = url.flatMap(URL.init(string:)) else { return }
        Task {
            guard let (data, _) = try? await URLSession.shared.data(from: url), let image = UIImage(data: data) else { return }
            await MainActor.run { item.setImage(image) }
        }
    }
}
