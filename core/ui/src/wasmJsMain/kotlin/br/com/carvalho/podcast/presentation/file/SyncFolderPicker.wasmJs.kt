package br.com.carvalho.podcast.presentation.file

import androidx.compose.runtime.Composable

@Composable
actual fun rememberSyncFolderPicker(onPicked: (bookmark: String, name: String) -> Unit): (() -> Unit)? = null
