package br.com.carvalho.podcast.presentation.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.HingePolicy
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective

/**
 * The panes of a tab never sit on a hinge (21.4): a Fold open flat has a crease down the middle that the default
 * policy ignores, since it does not separate the screen, so the list and the podcast meet at the fold instead.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun paneDirective(windowAdaptiveInfo: WindowAdaptiveInfo): PaneScaffoldDirective =
    calculatePaneScaffoldDirective(windowAdaptiveInfo, verticalHingePolicy = HingePolicy.AlwaysAvoid)
