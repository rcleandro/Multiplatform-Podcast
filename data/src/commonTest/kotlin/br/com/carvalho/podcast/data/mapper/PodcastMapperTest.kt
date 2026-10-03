package br.com.carvalho.podcast.data.mapper

import br.com.carvalho.podcast.data.local.entity.PodcastEntity
import br.com.carvalho.podcast.domain.model.Podcast
import kotlin.test.Test
import kotlin.test.assertEquals

class PodcastMapperTest {

    @Test
    fun `toDomain maps entity to domain model correctly`() {
        val entity = PodcastEntity(
            id = "id",
            title = "Title",
            description = "Desc",
            imageUrl = "img",
            author = "Author",
            language = "en",
            categories = """["Cat1", "Cat2"]""",
            feedUrl = "feed",
            siteUrl = "site",
            lastUpdated = 123L,
            isSubscribed = true
        )

        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(entity.title, domain.title)
        assertEquals(listOf("Cat1", "Cat2"), domain.categories)
    }

    @Test
    fun `toEntity maps domain model to entity correctly`() {
        val domain = Podcast(
            id = "id",
            title = "Title",
            description = "Desc",
            imageUrl = "img",
            author = "Author",
            language = "en",
            categories = listOf("Cat1", "Cat2"),
            feedUrl = "feed",
            siteUrl = "site",
            lastUpdated = 123L,
            isSubscribed = true
        )

        val entity = domain.toEntity()

        assertEquals(domain.id, entity.id)
        assertEquals("""["Cat1","Cat2"]""", entity.categories.replace(" ", ""))
    }
}
