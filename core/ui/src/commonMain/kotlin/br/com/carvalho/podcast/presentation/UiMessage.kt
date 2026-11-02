package br.com.carvalho.podcast.presentation

import org.jetbrains.compose.resources.StringResource

/** A one-off message for the user: a text from `composeResources` and the values for its placeholders. */
data class UiMessage(val text: StringResource, val args: List<Any> = emptyList())
