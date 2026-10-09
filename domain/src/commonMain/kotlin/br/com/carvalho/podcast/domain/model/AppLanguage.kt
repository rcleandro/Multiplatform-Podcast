package br.com.carvalho.podcast.domain.model

/** The language the app speaks (18.5): the system's, or one of those it is translated into. [tag] is BCP 47. */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    PORTUGUESE("pt"),
    SPANISH("es"),
    ;

    companion object {
        /** The choice a language tag ("pt-BR", "es") stands for; the system's when the app does not speak it. */
        fun of(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag != null && tag?.substringBefore('-')?.lowercase() == it.tag } ?: SYSTEM
    }
}
