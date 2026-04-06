package org.deafsapps.storeit.androidapp.presentation.rack.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.deafsapps.storeit.androidapp.R
import org.deafsapps.storeit.androidapp.design.Dimens
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import org.deafsapps.storeit.presentation.rack.model.RackSlotMarkerVo

@Composable
internal fun RackImageWithSlots(
    photoUri: String?,
    slots: List<RackSlotMarkerVo>,
    selectedSlot: RackSlotMarkerVo?,
    onTap: (xRel: Float, yRel: Float) -> Unit,
    onSlotMarkerDrag: (slotId: String, xRel: Float, yRel: Float) -> Unit,
    onSlotMarkerDragFinished: (
        slotId: String,
        initialXRel: Float,
        initialYRel: Float,
        finalXRel: Float,
        finalYRel: Float,
    ) -> Unit,
) {
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { imageSize = it },
    ) {
        if (photoUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photoUri)
                    .memoryCacheKey(photoUri)
                    .diskCacheKey(photoUri)
                    .build(),
                contentDescription = stringResource(R.string.rack_photo_content_description),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .size(Dimens.rackDetailPlaceholderHeight)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.rack_no_photo), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .testTag("rackDetailImageOverlay")
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val w = imageSize.width.toFloat().coerceAtLeast(1f)
                        val h = imageSize.height.toFloat().coerceAtLeast(1f)
                        onTap((offset.x / w).coerceIn(0f, 1f), (offset.y / h).coerceIn(0f, 1f))
                    }
                },
        )
        slots.forEach { slot ->
            var markerSize by remember(slot.id) { mutableStateOf(Dimens.rackDetailSlotMarkerSize) }
            var isDragging by remember(slot.id) { mutableStateOf(false) }
            val isSelected = selectedSlot?.id == slot.id
            val markerColor =
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
            val dragDrawAlpha = if (isDragging) getFlashAlpha() else 1f
            with(density) {
                val halfPx = Dimens.rackDetailSlotMarkerHalfSize.toPx()
                val xPx = (slot.xRel * imageSize.width - halfPx).toInt()
                val yPx = (slot.yRel * imageSize.height - halfPx).toInt()
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset { IntOffset(xPx, yPx) }
                        .size(markerSize)
                        .graphicsLayer(alpha = dragDrawAlpha)
                        .background(color = markerColor, shape = CircleShape)
                        .clickable(
                            interactionSource = remember(slot.id) { MutableInteractionSource() },
                            indication = null,
                        ) { onTap(slot.xRel, slot.yRel) }
                        .pointerInput(slot.id, imageSize) {
                            var dragXRel = 0f
                            var dragYRel = 0f
                            var initialXRel = 0f
                            var initialYRel = 0f
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    isDragging = true
                                    markerSize = Dimens.rackDetailDraggingSlotMarkerSize
                                    initialXRel = slot.xRel
                                    initialYRel = slot.yRel
                                    dragXRel = initialXRel
                                    dragYRel = initialYRel
                                },
                                onDrag = { _, dragAmount ->
                                    val w = imageSize.width.toFloat().coerceAtLeast(1f)
                                    val h = imageSize.height.toFloat().coerceAtLeast(1f)
                                    dragXRel = (dragXRel + dragAmount.x / w).coerceIn(0f, 1f)
                                    dragYRel = (dragYRel + dragAmount.y / h).coerceIn(0f, 1f)
                                    onSlotMarkerDrag(slot.id, dragXRel, dragYRel)
                                },
                                onDragEnd = {
                                    isDragging = false
                                    markerSize = Dimens.rackDetailSlotMarkerSize
                                    onSlotMarkerDragFinished(
                                        slot.id,
                                        initialXRel,
                                        initialYRel,
                                        dragXRel,
                                        dragYRel,
                                    )
                                },
                                onDragCancel = {
                                    isDragging = false
                                    markerSize = Dimens.rackDetailSlotMarkerSize
                                    onSlotMarkerDrag(slot.id, initialXRel, initialYRel)
                                },
                            )
                        },
                )
            }
        }
    }
}

@Composable
private fun getFlashAlpha(): Float {
    val flashTransition = rememberInfiniteTransition(label = "slotMarkerDrag")
    return flashTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 320, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "slotMarkerFlashAlpha",
    ).value
}
