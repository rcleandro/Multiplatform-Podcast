package br.com.carvalho.podcast.core.designsystem

/**
 * Color primitives of the "Sinal" identity (docs/adr/0001-identidade-visual.md). The number is the tone,
 * from 0 (black) to 100 (white). Only the color schemes and [PodcastColors] read these; screens use the roles.
 */
internal object Palette {
    // Warm neutrals: backgrounds, surfaces, text and outlines.
    const val BLACK = 0xFF000000
    const val NEUTRAL_4 = 0xFF110F0E
    const val NEUTRAL_6 = 0xFF161513
    const val NEUTRAL_10 = 0xFF1C1B19
    const val NEUTRAL_11 = 0xFF1D1B19
    const val NEUTRAL_12 = 0xFF22201D
    const val NEUTRAL_17 = 0xFF2C2A26
    const val NEUTRAL_20 = 0xFF31302D
    const val NEUTRAL_22 = 0xFF37342F
    const val NEUTRAL_24 = 0xFF3A3631
    const val NEUTRAL_25 = 0xFF3C3934
    const val NEUTRAL_40 = 0xFF605C55
    const val NEUTRAL_50 = 0xFF7D776E
    const val NEUTRAL_55 = 0xFF8A857C
    const val NEUTRAL_66 = 0xFFA8A398
    const val NEUTRAL_84 = 0xFFD6D2CA
    const val NEUTRAL_86 = 0xFFDCD9D3
    const val NEUTRAL_88 = 0xFFE0DDD7
    const val NEUTRAL_90 = 0xFFE6E4DF
    const val NEUTRAL_92 = 0xFFECEBE7
    const val NEUTRAL_94 = 0xFFF1EFEA
    const val NEUTRAL_95 = 0xFFF2F1EE
    const val NEUTRAL_96 = 0xFFF4F2EE
    const val NEUTRAL_97 = 0xFFF7F7F5
    const val WHITE = 0xFFFFFFFF

    // Brand amber.
    const val AMBER_10 = 0xFF2B1900
    const val AMBER_20 = 0xFF4F2F00
    const val AMBER_25 = 0xFF5A3600
    const val AMBER_40 = 0xFF9A5B00
    const val AMBER_50 = 0xFFBC7300
    const val AMBER_70 = 0xFFF2A93B
    const val AMBER_90 = 0xFFFCE3BC

    // Sepia, the secondary hue.
    const val SEPIA_10 = 0xFF2A2118
    const val SEPIA_30 = 0xFF4A3F33
    const val SEPIA_40 = 0xFF6E6152
    const val SEPIA_80 = 0xFFD6C6B3
    const val SEPIA_90 = 0xFFEFE4D6

    // Slate teal, the tertiary hue.
    const val SLATE_15 = 0xFF0E2A31
    const val SLATE_30 = 0xFF2E4C55
    const val SLATE_40 = 0xFF3F6872
    const val SLATE_80 = 0xFFA9CCD6
    const val SLATE_90 = 0xFFD4E6EB

    // Error red.
    const val RED_10 = 0xFF410E0B
    const val RED_20 = 0xFF690005
    const val RED_30 = 0xFF93000A
    const val RED_40 = 0xFFB3261E
    const val RED_80 = 0xFFFFB4AB
    const val RED_90 = 0xFFF9DEDC
    const val RED_92 = 0xFFFFDAD6

    // Green for the downloaded state.
    const val GREEN_40 = 0xFF2B7349
    const val GREEN_80 = 0xFF7FD6A0
}
