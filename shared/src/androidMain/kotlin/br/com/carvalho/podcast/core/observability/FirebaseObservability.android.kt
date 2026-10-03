package br.com.carvalho.podcast.core.observability

import com.google.firebase.FirebaseApp

internal actual fun isFirebaseConfigured(): Boolean = runCatching { FirebaseApp.getInstance() }.isSuccess
