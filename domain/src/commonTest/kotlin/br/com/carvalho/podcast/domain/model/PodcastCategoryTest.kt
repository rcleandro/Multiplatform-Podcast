package br.com.carvalho.podcast.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class PodcastCategoryTest {
    @Test
    fun aFeedsCategoriesBecomeAppleCategories() {
        // As the parser reads them: parent and subcategory, then another parent, in the feed's own spelling.
        val names = listOf("Technology", "Software How-To", "news", " Health & Fitness ", "Podcasting")

        assertEquals(
            setOf(PodcastCategory.TECHNOLOGY, PodcastCategory.NEWS, PodcastCategory.HEALTH_FITNESS),
            PodcastCategory.of(names),
        )
    }

    @Test
    fun everyCategoryHasItsOwnGenre() {
        assertEquals(PodcastCategory.entries.size, PodcastCategory.entries.map { it.genreId }.toSet().size)
    }
}
