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

public val Icons.Rounded.Inbox: ImageVector
    get() {
        if (_inbox != null) {
            return _inbox!!
        }
        _inbox = materialIcon(name = "Rounded.Inbox") {
            materialPath {
                moveTo(19.0f, 3.0f)
                horizontalLineTo(5.0f)
                curveTo(3.9f, 3.0f, 3.0f, 3.9f, 3.0f, 5.0f)
                verticalLineToRelative(14.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(14.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(5.0f)
                curveTo(21.0f, 3.9f, 20.1f, 3.0f, 19.0f, 3.0f)
                close()
                moveTo(19.0f, 5.0f)
                verticalLineToRelative(9.0f)
                horizontalLineToRelative(-3.56f)
                curveToRelative(-0.36f, 0.0f, -0.68f, 0.19f, -0.86f, 0.5f)
                curveTo(14.06f, 15.4f, 13.11f, 16.0f, 12.0f, 16.0f)
                reflectiveCurveToRelative(-2.06f, -0.6f, -2.58f, -1.5f)
                curveTo(9.24f, 14.19f, 8.91f, 14.0f, 8.56f, 14.0f)
                horizontalLineTo(5.0f)
                verticalLineTo(5.0f)
                horizontalLineTo(19.0f)
                close()
            }
        }
        return _inbox!!
    }

private var _inbox: ImageVector? = null
