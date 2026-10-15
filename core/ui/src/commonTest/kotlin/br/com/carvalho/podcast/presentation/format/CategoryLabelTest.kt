package br.com.carvalho.podcast.presentation.format

import br.com.carvalho.podcast.domain.model.PodcastCategory
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryLabelTest {
    /** A copied line in the mapping would show two categories under the same name. */
    @Test
    fun everyCategoryHasItsOwnName() {
        val labels = PodcastCategory.entries.map { it.label.key }

        assertEquals(PodcastCategory.entries.size, labels.toSet().size, labels.toString())
        assertEquals(PodcastCategory.entries.map { "category_" + it.name.lowercase() }, labels)
    }
}
