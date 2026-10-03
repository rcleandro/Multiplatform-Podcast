package br.com.carvalho.podcast.core.di

import org.koin.core.module.Module

/** What each platform creates itself: database, audio player, directories, analytics and crash reporting. */
expect val platformModule: Module
