package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.repository.FakePodcastRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeletePodcastUseCaseTest {
    private val podcastRepo = FakePodcastRepository()
    private val useCase = DeletePodcastUseCase(podcastRepo)

    @Test
    fun `calls repository delete method with correct id`() = runTest {
        val podcastId = "test-id"
        useCase(podcastId)
        assertEquals(podcastId, podcastRepo.deletePodcastCalledWith)
    }
}
