package br.com.carvalho.podcast.core.observability

import cocoapods.FirebaseCore.FIRApp
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
internal actual fun isFirebaseConfigured(): Boolean = FIRApp.defaultApp() != null
