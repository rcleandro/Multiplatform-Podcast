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

public val Icons.Rounded.RemoveDone: ImageVector
    get() {
        if (_removeDone != null) {
            return _removeDone!!
        }
        _removeDone = materialIcon(name = "Rounded.RemoveDone") {
            materialPath {
                moveTo(4.14f, 2.69f)
                lineTo(4.14f, 2.69f)
                curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0.0f, 1.41f)
                lineToRelative(9.67f, 9.67f)
                lineToRelative(-1.41f, 1.41f)
                lineToRelative(-3.54f, -3.53f)
                curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                lineToRelative(0.0f, 0.0f)
                curveToRelative(-0.39f, 0.39f, -0.39f, 1.02f, 0.0f, 1.41f)
                lineToRelative(4.24f, 4.24f)
                curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                lineToRelative(2.12f, -2.12f)
                lineToRelative(5.89f, 5.89f)
                curveToRelative(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0.0f)
                lineToRelative(0.0f, 0.0f)
                curveToRelative(0.39f, -0.39f, 0.39f, -1.02f, 0.0f, -1.41f)
                lineTo(5.55f, 2.69f)
                curveTo(5.16f, 2.3f, 4.53f, 2.3f, 4.14f, 2.69f)
                close()
                moveTo(18.05f, 12.36f)
                lineToRelative(4.24f, -4.24f)
                curveToRelative(0.39f, -0.39f, 0.39f, -1.03f, -0.01f, -1.42f)
                lineToRelative(0.0f, 0.0f)
                curveToRelative(-0.39f, -0.38f, -1.02f, -0.38f, -1.41f, 0.01f)
                lineToRelative(-4.24f, 4.24f)
                lineTo(18.05f, 12.36f)
                close()
                moveTo(16.64f, 6.7f)
                lineTo(16.64f, 6.7f)
                curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                lineToRelative(-1.42f, 1.42f)
                lineToRelative(1.41f, 1.41f)
                lineToRelative(1.42f, -1.42f)
                curveTo(17.03f, 7.72f, 17.03f, 7.09f, 16.64f, 6.7f)
                close()
                moveTo(1.79f, 13.06f)
                lineToRelative(4.95f, 4.95f)
                lineToRelative(1.41f, -1.41f)
                lineTo(3.2f, 11.65f)
                curveToRelative(-0.39f, -0.39f, -1.02f, -0.39f, -1.41f, 0.0f)
                lineToRelative(0.0f, 0.0f)
                curveTo(1.4f, 12.04f, 1.4f, 12.67f, 1.79f, 13.06f)
                close()
            }
        }
        return _removeDone!!
    }

private var _removeDone: ImageVector? = null
