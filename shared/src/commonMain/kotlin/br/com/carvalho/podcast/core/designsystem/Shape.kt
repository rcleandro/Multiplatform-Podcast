package br.com.carvalho.podcast.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(AppDimensions.radiusExtraSmall),
    small = RoundedCornerShape(AppDimensions.radiusSmall),
    medium = RoundedCornerShape(AppDimensions.radiusMedium),
    large = RoundedCornerShape(AppDimensions.radiusLarge),
    extraLarge = RoundedCornerShape(AppDimensions.radiusExtraLarge)
)
