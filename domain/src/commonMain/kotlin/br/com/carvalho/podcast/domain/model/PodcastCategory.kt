package br.com.carvalho.podcast.domain.model

/**
 * Apple Podcasts' top-level categories (18.2). Feeds name them in English in `itunes:category` ([appleName]), and the
 * directory ranks podcasts per category by [genreId]. Subcategories ("Software How-To") come with their parent, which
 * is what counts here.
 */
@Suppress("MagicNumber") // Apple's genre ids are identifiers, not tuning values
enum class PodcastCategory(val appleName: String, val genreId: Int) {
    ARTS("Arts", 1301),
    BUSINESS("Business", 1321),
    COMEDY("Comedy", 1303),
    EDUCATION("Education", 1304),
    FICTION("Fiction", 1483),
    GOVERNMENT("Government", 1511),
    HEALTH_FITNESS("Health & Fitness", 1512),
    HISTORY("History", 1487),
    KIDS_FAMILY("Kids & Family", 1305),
    LEISURE("Leisure", 1502),
    MUSIC("Music", 1310),
    NEWS("News", 1489),
    RELIGION_SPIRITUALITY("Religion & Spirituality", 1314),
    SCIENCE("Science", 1533),
    SOCIETY_CULTURE("Society & Culture", 1324),
    SPORTS("Sports", 1545),
    TECHNOLOGY("Technology", 1318),
    TRUE_CRIME("True Crime", 1488),
    TV_FILM("TV & Film", 1309),
    ;

    companion object {
        /** The categories among a feed's [names]; unknown or misspelled names are left out. */
        fun of(names: List<String>): Set<PodcastCategory> = names.mapNotNullTo(mutableSetOf()) { name ->
            entries.firstOrNull { it.appleName.equals(name.trim(), ignoreCase = true) }
        }
    }
}

val Podcast.topCategories: Set<PodcastCategory> get() = PodcastCategory.of(categories)
