package br.com.carvalho.podcast.data.local.entity

import androidx.room3.Entity
import androidx.room3.Fts4
import androidx.room3.FtsOptions

/**
 * The full-text index of the episodes' titles and descriptions (23.4), kept in step with `episodes` by the triggers
 * Room makes for an external-content table. `LIKE '%…%'` read the whole table on every key, 124 to 169 ms per query on
 * a Razr 60 with 13,077 episodes; a match here takes under 1 ms. unicode61 without diacritics finds "ação" from "acao".
 */
@Fts4(
    contentEntity = EpisodeEntity::class,
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
    tokenizerArgs = ["remove_diacritics=1"],
)
@Entity(tableName = "episodes_fts")
data class EpisodeFtsEntity(
    val title: String,
    val description: String?,
)
