package br.com.carvalho.podcast.core.util

import android.content.Context

val AppContext.androidContext: Context
    get() = context as Context
