package br.com.carvalho.podcast.data.local
expect fun createInMemoryDatabase(): AppDatabase

expect val isDatabaseSupported: Boolean
