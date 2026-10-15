package br.com.carvalho.podcast.core.util

import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale

actual fun deviceCountry(): String? = NSLocale.currentLocale.countryCode
