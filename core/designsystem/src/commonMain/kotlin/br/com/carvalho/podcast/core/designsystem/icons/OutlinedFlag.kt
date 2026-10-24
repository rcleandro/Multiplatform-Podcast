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

public val Icons.Rounded.OutlinedFlag: ImageVector
    get() {
        if (_outlinedFlag != null) {
            return _outlinedFlag!!
        }
        _outlinedFlag = materialIcon(name = "Rounded.OutlinedFlag") {
            materialPath {
                moveTo(14.0f, 6.0f)
                lineToRelative(-0.72f, -1.45f)
                curveToRelative(-0.17f, -0.34f, -0.52f, -0.55f, -0.9f, -0.55f)
                lineTo(6.0f, 4.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                verticalLineToRelative(15.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                verticalLineToRelative(-6.0f)
                horizontalLineToRelative(5.0f)
                lineToRelative(0.72f, 1.45f)
                curveToRelative(0.17f, 0.34f, 0.52f, 0.55f, 0.89f, 0.55f)
                lineTo(19.0f, 16.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                lineTo(20.0f, 7.0f)
                curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                horizontalLineToRelative(-5.0f)
                close()
                moveTo(18.0f, 14.0f)
                horizontalLineToRelative(-4.0f)
                lineToRelative(-1.0f, -2.0f)
                lineTo(7.0f, 12.0f)
                lineTo(7.0f, 6.0f)
                horizontalLineToRelative(5.0f)
                lineToRelative(1.0f, 2.0f)
                horizontalLineToRelative(5.0f)
                verticalLineToRelative(6.0f)
                close()
            }
        }
        return _outlinedFlag!!
    }

private var _outlinedFlag: ImageVector? = null
