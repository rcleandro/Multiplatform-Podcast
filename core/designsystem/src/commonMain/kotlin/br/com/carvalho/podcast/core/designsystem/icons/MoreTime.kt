/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Copied from material-icons-extended 1.7.3 (23.9): the app uses these few, not the 7,000 it ships.
package br.com.carvalho.podcast.core.designsystem.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Rounded.MoreTime: ImageVector
    get() {
        if (_moreTime != null) {
            return _moreTime!!
        }
        _moreTime = materialIcon(name = "Rounded.MoreTime") {
            materialPath {
                moveTo(10.75f, 8.0f)
                curveTo(10.34f, 8.0f, 10.0f, 8.34f, 10.0f, 8.75f)
                verticalLineToRelative(4.69f)
                curveToRelative(0.0f, 0.35f, 0.18f, 0.67f, 0.47f, 0.85f)
                lineToRelative(3.64f, 2.24f)
                curveToRelative(0.33f, 0.2f, 0.76f, 0.11f, 0.97f, -0.21f)
                curveToRelative(0.23f, -0.34f, 0.12f, -0.8f, -0.23f, -1.01f)
                lineTo(11.5f, 13.3f)
                verticalLineTo(8.75f)
                curveTo(11.5f, 8.34f, 11.16f, 8.0f, 10.75f, 8.0f)
                close()
            }
            materialPath {
                moveTo(17.92f, 12.0f)
                curveToRelative(0.05f, 0.33f, 0.08f, 0.66f, 0.08f, 1.0f)
                curveToRelative(0.0f, 3.9f, -3.1f, 7.0f, -7.0f, 7.0f)
                reflectiveCurveToRelative(-7.0f, -3.1f, -7.0f, -7.0f)
                curveToRelative(0.0f, -3.9f, 3.1f, -7.0f, 7.0f, -7.0f)
                curveToRelative(0.7f, 0.0f, 1.37f, 0.1f, 2.0f, 0.29f)
                verticalLineTo(4.23f)
                curveTo(12.36f, 4.08f, 11.69f, 4.0f, 11.0f, 4.0f)
                curveToRelative(-5.0f, 0.0f, -9.0f, 4.0f, -9.0f, 9.0f)
                reflectiveCurveToRelative(4.0f, 9.0f, 9.0f, 9.0f)
                reflectiveCurveToRelative(9.0f, -4.0f, 9.0f, -9.0f)
                curveToRelative(0.0f, -0.34f, -0.02f, -0.67f, -0.06f, -1.0f)
                horizontalLineTo(17.92f)
                close()
            }
            materialPath {
                moveTo(22.0f, 5.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineTo(3.0f)
                curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                reflectiveCurveToRelative(-1.0f, 0.45f, -1.0f, 1.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(-2.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(2.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                verticalLineTo(7.0f)
                horizontalLineToRelative(2.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                curveTo(23.0f, 5.45f, 22.55f, 5.0f, 22.0f, 5.0f)
                close()
            }
        }
        return _moreTime!!
    }

private var _moreTime: ImageVector? = null
