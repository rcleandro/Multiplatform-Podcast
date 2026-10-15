package br.com.carvalho.podcast.presentation.format

import br.com.carvalho.podcast.core.ui.generated.resources.Res
import br.com.carvalho.podcast.core.ui.generated.resources.category_arts
import br.com.carvalho.podcast.core.ui.generated.resources.category_business
import br.com.carvalho.podcast.core.ui.generated.resources.category_comedy
import br.com.carvalho.podcast.core.ui.generated.resources.category_education
import br.com.carvalho.podcast.core.ui.generated.resources.category_fiction
import br.com.carvalho.podcast.core.ui.generated.resources.category_government
import br.com.carvalho.podcast.core.ui.generated.resources.category_health_fitness
import br.com.carvalho.podcast.core.ui.generated.resources.category_history
import br.com.carvalho.podcast.core.ui.generated.resources.category_kids_family
import br.com.carvalho.podcast.core.ui.generated.resources.category_leisure
import br.com.carvalho.podcast.core.ui.generated.resources.category_music
import br.com.carvalho.podcast.core.ui.generated.resources.category_news
import br.com.carvalho.podcast.core.ui.generated.resources.category_religion_spirituality
import br.com.carvalho.podcast.core.ui.generated.resources.category_science
import br.com.carvalho.podcast.core.ui.generated.resources.category_society_culture
import br.com.carvalho.podcast.core.ui.generated.resources.category_sports
import br.com.carvalho.podcast.core.ui.generated.resources.category_technology
import br.com.carvalho.podcast.core.ui.generated.resources.category_true_crime
import br.com.carvalho.podcast.core.ui.generated.resources.category_tv_film
import br.com.carvalho.podcast.domain.model.PodcastCategory
import org.jetbrains.compose.resources.StringResource

/** The category's name in the user's language, the one Apple Podcasts shows. */
val PodcastCategory.label: StringResource get() = when (this) {
    PodcastCategory.ARTS -> Res.string.category_arts
    PodcastCategory.BUSINESS -> Res.string.category_business
    PodcastCategory.COMEDY -> Res.string.category_comedy
    PodcastCategory.EDUCATION -> Res.string.category_education
    PodcastCategory.FICTION -> Res.string.category_fiction
    PodcastCategory.GOVERNMENT -> Res.string.category_government
    PodcastCategory.HEALTH_FITNESS -> Res.string.category_health_fitness
    PodcastCategory.HISTORY -> Res.string.category_history
    PodcastCategory.KIDS_FAMILY -> Res.string.category_kids_family
    PodcastCategory.LEISURE -> Res.string.category_leisure
    PodcastCategory.MUSIC -> Res.string.category_music
    PodcastCategory.NEWS -> Res.string.category_news
    PodcastCategory.RELIGION_SPIRITUALITY -> Res.string.category_religion_spirituality
    PodcastCategory.SCIENCE -> Res.string.category_science
    PodcastCategory.SOCIETY_CULTURE -> Res.string.category_society_culture
    PodcastCategory.SPORTS -> Res.string.category_sports
    PodcastCategory.TECHNOLOGY -> Res.string.category_technology
    PodcastCategory.TRUE_CRIME -> Res.string.category_true_crime
    PodcastCategory.TV_FILM -> Res.string.category_tv_film
}
