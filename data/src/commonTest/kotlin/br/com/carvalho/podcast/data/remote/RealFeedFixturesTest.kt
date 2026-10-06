package br.com.carvalho.podcast.data.remote

import br.com.carvalho.podcast.core.util.episodeId
import br.com.carvalho.podcast.data.mapper.toEpisode
import br.com.carvalho.podcast.data.remote.model.RssFeed
import br.com.carvalho.podcast.domain.model.Episode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Reads the feeds in `src/commonTest/resources/feeds` (real ones, trimmed, plus one synthetic) and compares with
 * values taken from them by a standard XML parser. Each test covers one field, so a parser change shows exactly
 * what it fixes or breaks.
 */
class RealFeedFixturesTest {

    @Test
    fun everyFixtureHasExpectations() {
        assertEquals(EXPECTED.map { it.fixture }.sorted(), FeedFixtures.all.keys.sorted())
    }

    @Test
    fun channelTitles() = check { expected, feed, _ ->
        listOf(Triple("title", expected.title, feed.title))
    }

    @Test
    fun episodeCounts() = check { expected, _, episodes ->
        listOf(Triple("episodes", expected.episodes.size, episodes.count { it.audioUrl.isNotEmpty() }))
    }

    // 15.4: an item without <enclosure> becomes an episode with an empty audio URL.
    @Test
    fun itemsWithoutAudioAreSkipped() = check(knownBroken = setOf(SYNTHETIC)) { _, _, episodes ->
        listOf(Triple("episodes without audio", 0, episodes.count { it.audioUrl.isEmpty() }))
    }

    @Test
    fun episodeTitles() = checkEpisodes { expected, actual ->
        Triple("title", expected.title, actual.title)
    }

    @Test
    fun guids() = checkEpisodes { expected, actual ->
        Triple("id from guid ${expected.guid}", episodeId(PODCAST_ID, expected.guid, expected.audioUrl), actual.id)
    }

    @Test
    fun audioUrls() = checkEpisodes { expected, actual -> Triple("audio", expected.audioUrl, actual.audioUrl) }

    // 15.3: the time zone is ignored and every date is read as UTC.
    @Test
    fun publishDates() = checkEpisodes(knownBroken = setOf(SYNTHETIC, "buzzsprout-buzzcast")) { expected, actual ->
        Triple("date", expected.publishDate, actual.publishDate)
    }

    @Test
    fun durations() = checkEpisodes { expected, actual ->
        Triple("duration", expected.duration, actual.duration)
    }

    // knownBroken: fixtures a later roadmap item fixes; that item removes them from here, so its test fails first.
    private fun check(
        knownBroken: Set<String> = emptySet(),
        compare: (Expected, RssFeed, List<Episode>) -> List<Triple<String, Any?, Any?>>,
    ) {
        val mismatches = EXPECTED.filter { it.fixture !in knownBroken }.flatMap { expected ->
            val feed = RssXmlParser.parse(FeedFixtures.all.getValue(expected.fixture))
            compare(expected, feed, feed.episodes.map { it.toEpisode(PODCAST_ID) })
                .filter { (_, want, got) -> want != got }
                .map { (field, want, got) -> "${expected.fixture} $field: expected <$want>, got <$got>" }
        }
        assertTrue(mismatches.isEmpty(), mismatches.joinToString("\n"))
    }

    // Episodes are paired in order, ignoring the ones without audio (that is itemsWithoutAudioAreSkipped's job).
    private fun checkEpisodes(
        knownBroken: Set<String> = emptySet(),
        compare: (Ep, Episode) -> Triple<String, Any?, Any?>,
    ) = check(knownBroken) { expected, _, episodes ->
        expected.episodes.zip(episodes.filter { it.audioUrl.isNotEmpty() }).mapIndexed { index, (want, got) ->
            compare(want, got).let { (field, a, b) -> Triple("#$index $field", a, b) }
        }
    }

    private class Expected(val fixture: String, val title: String, vararg val episodes: Ep)

    private class Ep(val title: String, val guid: String?, val audioUrl: String, val publishDate: Long, val duration: Long)

    private companion object {
        const val PODCAST_ID = "https://feeds.example.com/rss"
        const val SYNTHETIC = "synthetic-edge-cases"

        // Generated from the fixtures with Python's xml.etree and email.utils.parsedate_to_datetime.
        val EXPECTED = listOf(
        Expected(
            "anchor-codigo-fonte", "Compilado do Código Fonte TV",
            Ep("Criaram o Angular Native; Cloudflare lança rival do Jev; Anthropic amarga prejuízo bilionário; Agentes vazam prints de sistemas no GitHub; EUA firma pacto de segurança com IA [Compilado #265]", "901e84f0-b222-4ac8-800c-c0aeb0c7a737", "https://anchor.fm/s/4f366e84/podcast/play/126758849/https%3A%2F%2Fd3ctxlq1ktw2nl.cloudfront.net%2Fstaging%2F2026-9-3%2F433255267-44100-2-d675877058bf6.mp3", 1791111600000L, 4297L),
            Ep("Amazon recontrata especialistas em IA; Copilot em Rust; Gemini Hackeia Empresas; AGENTS.md no Claude Code; Novidades PHP", "9a48f3ca-bdef-42d3-a8ea-f4fcb7e58bfd", "https://anchor.fm/s/4f366e84/podcast/play/126347150/https%3A%2F%2Fd3ctxlq1ktw2nl.cloudfront.net%2Fstaging%2F2026-8-26%2F432719332-44100-2-6d38f3ac2351f.mp3", 1790506800000L, 4739L),
        ),
        Expected(
            "buzzsprout-buzzcast", "Buzzcast",
            Ep("How Teens Turn Podcast Episodes Into Secret Chat Rooms", "Buzzsprout-19896240", "https://op3.dev/e/https://dts.podtrac.com/redirect.mp3/www.buzzsprout.com/231452/episodes/19896240-how-teens-turn-podcast-episodes-into-secret-chat-rooms.mp3", 1790942400000L, 900L),
            Ep("Keeping The Indie Spirit Alive As Podcasting Grows", "Buzzsprout-19861008", "https://op3.dev/e/https://dts.podtrac.com/redirect.mp3/www.buzzsprout.com/231452/episodes/19861008-keeping-the-indie-spirit-alive-as-podcasting-grows.mp3", 1790337600000L, 3793L),
            Ep("6 Episode Title Tips For Podcast Growth", "Buzzsprout-18803794", "https://op3.dev/e/https://dts.podtrac.com/redirect.mp3/www.buzzsprout.com/231452/episodes/18803794-6-episode-title-tips-for-podcast-growth.mp3", 1772834400000L, 1069L),
        ),
        Expected(
            "hipsters", "Hipsters Ponto Tech",
            Ep("Paulo Silveira comenta: Orquestração, arquétipos e profundidade – Hipsters Ponto Tech #536", "https://www.hipsters.tech/?p=6275", "https://media.blubrry.com/hipsterstech/content.blubrry.com/hipsterstech/HpT-536.mp3", 1791257761000L, 1656L),
            Ep("Liderando equipes de tecnologia – Hipsters Ponto Tech #535", "https://www.hipsters.tech/?p=6271", "https://media.blubrry.com/hipsterstech/content.blubrry.com/hipsterstech/HpT-535.mp3", 1790654352000L, 2115L),
            Ep("O SaaS está morto? – Hipsters Ponto Tech #515", "https://www.hipsters.tech/?p=6111", "https://media.blubrry.com/hipsterstech/content.blubrry.com/hipsterstech/HpT-515.mp3", 1778560590000L, 4153L),
        ),
        Expected(
            "hoyhablamos-basico", "Hoy Hablamos Básico: Aprender español nivel básico-intermedio | Learn Spanish",
            Ep("140. El café", "https://www.hoyhablamos.com/?p=267659", "https://media.blubrry.com/podcast_diario_de_espaol/media.blubrry.com/3715649/content.blubrry.com/3715649/HHB_140_El_cafe_mezcla.mp3", 1790955697000L, 666L),
            Ep("139. El otoño", "https://www.hoyhablamos.com/?p=267548", "https://media.blubrry.com/podcast_diario_de_espaol/media.blubrry.com/3715649/content.blubrry.com/3715649/HHB_139_El_oton_o_mezcla.mp3", 1790344865000L, 602L),
            Ep("26. Mal de amores", "https://www.hoyhablamos.com/?p=249813", "https://media.blubrry.com/podcast_diario_de_espaol/media.blubrry.com/3715649/content.blubrry.com/3715649/HHB_episodio_26_Mal_de_amores_mezcla.mp3", 1721839046000L, 665L),
        ),
        Expected(
            "hoyhablamos-podcast", "Hoy Hablamos: Podcast diario para aprender español - Learn Spanish Daily Podcast",
            Ep("2375. Maricarmen y elecciones generales", "https://www.hoyhablamos.com/?p=267733", "https://media.blubrry.com/podcast_diario_de_espaol/content.blubrry.com/podcast_diario_de_espaol/Episodio_2375_Maricarmen_y_las_elecciones_generales_mezcla.mp3", 1791313288000L, 1080L),
            Ep("2374. La sonda Mars Climate Orbiter", "https://www.hoyhablamos.com/?p=267699", "https://media.blubrry.com/podcast_diario_de_espaol/content.blubrry.com/podcast_diario_de_espaol/Episodio_2374_La_sonda_Mars_Climate_Orbiter_mezcla.mp3", 1791227633000L, 664L),
        ),
        Expected(
            "libsyn-ask-a-spaceman", "Ask a Spaceman!",
            Ep("AaS! 281: What Makes the Scattered Disk So Wild?", "b4ef3a52-1b75-467c-b616-9855f18add44", "https://dts.podtrac.com/redirect.mp3/pscrb.fm/rss/p/clrtpod.com/m/mgln.ai/e/35/traffic.libsyn.com/secure/askaspaceman/AAS281_10012026_AdBR04m21sTO04m27s.mp3?dest-id=234448", 1791284400000L, 1624L),
            Ep("AaS! 280: Wait So We Can Steal Energy from Black Holes?", "11da6f1f-92c2-4c7a-ac3e-dbc97e4bf1d5", "https://dts.podtrac.com/redirect.mp3/pscrb.fm/rss/p/clrtpod.com/m/mgln.ai/e/35/traffic.libsyn.com/secure/askaspaceman/AAS280_09152026_AdBR19m58sTO20m0s.mp3?dest-id=234448", 1790074800000L, 1635L),
            Ep("AaS! 172: What Are Some Alternatives to the Big Bang Theory?", "729abc08-de8d-48e1-8a24-d8d559260950", "https://dts.podtrac.com/redirect.mp3/pscrb.fm/rss/p/clrtpod.com/m/mgln.ai/e/35/traffic.libsyn.com/secure/askaspaceman/aas172.mp3?dest-id=234448", 1647342000000L, 2136L),
        ),
        Expected(
            "nerdcast", "NerdCast",
            Ep("NerdCast 1050 - O Melhor de 1050 NerdCasts", "b34eab96-be87-11f1-b92c-4f672efc0dcf", "https://pdst.fm/e/traffic.megaphone.fm/JNPD5526109524.mp3", 1790962500000L, 10452L),
            Ep("NerdCast 1049 - NerdCon 2026: Ao Vivo, Tudo Pode Acontecer!", "a7a63d16-b90c-11f1-a8f4-a7b9d79be52c", "https://pdst.fm/e/traffic.megaphone.fm/JNPD3737840812.mp3", 1790359920000L, 5577L),
        ),
        Expected(
            "npr-planet-money", "Planet Money",
            Ep("Where all those weird new drinks are coming from", "f35873ff-fecc-4857-bc2d-0f8a1a1202d5", "https://tracking.swap.fm/track/XvDEoI11TR00olTUO8US/prfx.byspotify.com/e/play.podtrac.com/npr-510289/npr.simplecastaudio.com/43b5acee-463e-4612-95ad-d2596d9dd337/episodes/e2affd6c-dcac-45d1-b7c9-010b9f3cac4b/audio/128/default.mp3?awCollectionId=43b5acee-463e-4612-95ad-d2596d9dd337&awEpisodeId=e2affd6c-dcac-45d1-b7c9-010b9f3cac4b&feed=hvWWWzRv&t=podcast&e=nx-s1-5989651&p=510289&d=1780&size=28490275", 1790982861000L, 1780L),
            Ep("Who’s gonna pay for your Social Security?", "84e80865-2556-470a-9f62-d1106c5aede6", "https://tracking.swap.fm/track/XvDEoI11TR00olTUO8US/prfx.byspotify.com/e/play.podtrac.com/npr-510289/npr.simplecastaudio.com/43b5acee-463e-4612-95ad-d2596d9dd337/episodes/47d744ef-f06a-4638-936e-6eea1d635b37/audio/128/default.mp3?awCollectionId=43b5acee-463e-4612-95ad-d2596d9dd337&awEpisodeId=47d744ef-f06a-4638-936e-6eea1d635b37&feed=hvWWWzRv&t=podcast&e=nx-s1-5985851&p=510289&d=1753&size=28062284", 1790780143000L, 1753L),
        ),
        Expected(
            "podcasting20", "Podcasting 2.0",
            Ep("Episode 273: \"Advertising is the Horse and Buggies of Podcasting\"", "PC20-273", "https://op3.dev/e/mp3s.nashownotes.com/PC20-273-2026-10-02-Final.mp3", 1790968688000L, 5385L),
            Ep("Episode 272: \"Is this Y'all?\"", "PC20-272", "https://op3.dev/e/mp3s.nashownotes.com/PC20-272-2026-09-25-Final.mp3", 1790363748000L, 5501L),
        ),
        Expected(
            "simplecast-the-daily", "The Daily",
            Ep("Call My A.I. Agent", "33dce268-3492-41c6-bc48-1ea528cd2b51", "https://dts.podtrac.com/redirect.mp3/pdst.fm/e/pfx.vpixl.com/6qj4J/pscrb.fm/rss/p/nyt.simplecastaudio.com/03d8b493-87fc-4bd1-931f-8a8e9b945d8a/episodes/367aa3e9-2eaa-435f-867a-f172c3ef67cf/audio/128/default.mp3?aid=rss_feed&awCollectionId=03d8b493-87fc-4bd1-931f-8a8e9b945d8a&awEpisodeId=367aa3e9-2eaa-435f-867a-f172c3ef67cf&feed=54nAGcIl", 1791279981000L, 1670L),
            Ep("Midterm Surprise: Democrats Are Surging in Red America", "659a39f7-9763-4d45-bd48-825f1167e5ee", "https://dts.podtrac.com/redirect.mp3/pdst.fm/e/pfx.vpixl.com/6qj4J/pscrb.fm/rss/p/nyt.simplecastaudio.com/03d8b493-87fc-4bd1-931f-8a8e9b945d8a/episodes/717e8887-4828-4b69-8249-f58831811f43/audio/128/default.mp3?aid=rss_feed&awCollectionId=03d8b493-87fc-4bd1-931f-8a8e9b945d8a&awEpisodeId=717e8887-4828-4b69-8249-f58831811f43&feed=54nAGcIl", 1791193500000L, 1656L),
        ),
        Expected(
            "synthetic-edge-cases", "Edge Cases & Friends",
            Ep("Rock & Roll – part 1", "edge-1", "https://cdn.example.com/ep1.mp3?a=1&b=2", 1791214200000L, 3725L),
            Ep("No guid", null, "https://cdn.example.com/ep2.m4a", 1791165600000L, 2707L),
        ),
        )
    }
}
