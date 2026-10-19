package br.com.carvalho.podcast.presentation.file

import androidx.compose.runtime.Composable

/**
 * Opens the system's folder picker for library sync (19.12) and hands over the folder's lasting permission (a
 * bookmark) and its name; nothing when the user gives up. Null where the app cannot keep a folder: the Web, and iOS
 * until its own player (phase 25).
 */
@Composable
expect fun rememberSyncFolderPicker(onPicked: (bookmark: String, name: String) -> Unit): (() -> Unit)?
