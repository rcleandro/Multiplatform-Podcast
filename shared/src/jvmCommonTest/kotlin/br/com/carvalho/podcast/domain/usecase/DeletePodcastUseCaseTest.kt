package br.com.carvalho.podcast.domain.usecase

import br.com.carvalho.podcast.domain.repository.PodcastRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DeletePodcastUseCaseTest {
    private val podcastRepo = mockk<PodcastRepository>(relaxed = true)
    private val useCase = DeletePodcastUseCase(podcastRepo)

    @Test
    fun `calls repository delete method with correct id`() = runTest {
        val podcastId = "test-id"
        useCase(podcastId)
        coVerify { podcastRepo.deletePodcast(podcastId) }
    }
}
