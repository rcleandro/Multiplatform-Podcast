package br.com.carvalho.podcast.domain.model

/** The language the app speaks (18.5): the system's, or one of those it is translated into. [tag] is BCP 47. */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    PORTUGUESE("pt"),
    SPANISH("es"),
    FRENCH("fr"),
    GERMAN("de"),
    ITALIAN("it"),
    JAPANESE("ja"),
    KOREAN("ko"),
    CHINESE("zh"),
    HINDI("hi"),
    ARABIC("ar"),
    INDONESIAN("id"),
    RUSSIAN("ru"),
    TURKISH("tr"),
    ;

    companion object {
        /**
         * The choice a language tag ("pt-BR", "es") stands for; the system's when the app does not speak it. Android
         * still names Indonesian "in", its code before 1989.
         */
        fun of(tag: String?): AppLanguage {
            val language = tag?.substringBefore('-')?.lowercase()?.let { if (it == "in") "id" else it }
            return entries.firstOrNull { it.tag != null && language == it.tag } ?: SYSTEM
        }
    }
}
