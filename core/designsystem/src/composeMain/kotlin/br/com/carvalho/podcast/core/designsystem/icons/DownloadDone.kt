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

public val Icons.Rounded.DownloadDone: ImageVector
    get() {
        if (_downloadDone != null) {
            return _downloadDone!!
        }
        _downloadDone = materialIcon(name = "Rounded.DownloadDone") {
            materialPath {
                moveTo(6.0f, 18.0f)
                horizontalLineToRelative(12.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                reflectiveCurveToRelative(-0.45f, 1.0f, -1.0f, 1.0f)
                lineTo(6.0f, 20.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
                reflectiveCurveToRelative(0.45f, -1.0f, 1.0f, -1.0f)
                close()
                moveTo(11.01f, 13.9f)
                curveToRelative(-0.78f, 0.77f, -2.04f, 0.77f, -2.82f, -0.01f)
                lineTo(6.0f, 11.7f)
                curveToRelative(-0.55f, -0.55f, -0.54f, -1.44f, 0.03f, -1.97f)
                curveToRelative(0.54f, -0.52f, 1.4f, -0.5f, 1.92f, 0.02f)
                lineTo(9.6f, 11.4f)
                lineToRelative(6.43f, -6.43f)
                curveToRelative(0.54f, -0.54f, 1.41f, -0.54f, 1.95f, 0.0f)
                lineToRelative(0.04f, 0.04f)
                curveToRelative(0.54f, 0.54f, 0.54f, 1.42f, -0.01f, 1.96f)
                lineToRelative(-7.0f, 6.93f)
                close()
            }
        }
        return _downloadDone!!
    }

private var _downloadDone: ImageVector? = null
