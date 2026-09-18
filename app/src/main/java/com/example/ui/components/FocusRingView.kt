package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CameraYellow
import kotlin.math.roundToInt

@Composable
fun FocusRingView(
  focusOffset: Offset,
  exposureValue: Int,
  exposureRange: IntRange,
  onExposureAdjusted: (Float) -> Unit,
  modifier: Modifier = Modifier
) {
  val density = LocalDensity.current
  val scale = remember { Animatable(1.3f) }

  LaunchedEffect(focusOffset) {
    scale.snapTo(1.3f)
    scale.animateTo(1.0f, animationSpec = tween(220))
  }

  val ringSize = 72.dp
  val ringSizePx = with(density) { ringSize.toPx() }

  Box(
    modifier = modifier
      .offset {
        IntOffset(
          (focusOffset.x - ringSizePx / 2f).roundToInt(),
          (focusOffset.y - ringSizePx / 2f).roundToInt()
        )
      }
      .size(ringSize)
      .testTag("focus_metering_ring"),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.size(ringSize)) {
      val r = (size.minDimension / 2f - 4f) * scale.value
      val center = Offset(size.width / 2f, size.height / 2f)

      // Outer focus circle
      drawCircle(
        color = CameraYellow,
        radius = r,
        center = center,
        style = Stroke(width = 1.8.dp.toPx())
      )

      // Center crosshair / dot
      drawCircle(
        color = CameraYellow,
        radius = 2.dp.toPx(),
        center = center
      )
    }

    // Exposure Slider indicator handle
    Box(
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .offset(x = 24.dp)
        .size(36.dp)
        .pointerInput(Unit) {
          detectVerticalDragGestures { _, dragAmount ->
            // Drag up: increase exposure; Drag down: decrease exposure
            onExposureAdjusted(-dragAmount)
          }
        }
        .testTag("exposure_slider_handle"),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.WbSunny,
        contentDescription = "Exposure Adjustment",
        tint = CameraYellow,
        modifier = Modifier.size(20.dp)
      )
    }
  }
}
