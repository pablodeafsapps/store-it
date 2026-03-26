package org.deafsapps.storeit.androidapp.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val BackArrowIcon: ImageVector =
    ImageVector.Builder(
        name = "BackArrowIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // Material "ArrowBack" style: back arrow suitable for use as a TopAppBar navigation icon.
        path(
            fill = SolidColor(Color.Black),
        ) {
            moveTo(20.0f, 11.0f)
            horizontalLineTo(7.83f)
            lineTo(13.41f, 5.41f)
            lineTo(12.0f, 4.0f)
            lineTo(4.0f, 12.0f)
            lineTo(12.0f, 20.0f)
            lineTo(13.41f, 18.59f)
            lineTo(7.83f, 13.0f)
            horizontalLineTo(20.0f)
            verticalLineTo(11.0f)
        }
    }.build()
