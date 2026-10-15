package br.com.carvalho.podcast.core.util

/** The device's region as an ISO 3166 code ("BR", "US"), or null when the system does not say. */
expect fun deviceCountry(): String?
