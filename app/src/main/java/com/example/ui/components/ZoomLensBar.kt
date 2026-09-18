package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LensOption
import com.example.ui.theme.CameraTextMuted
import com.example.ui.theme.CameraTextWhite
import com.example.ui.theme.CameraYellow

@Composable
fun ZoomLensBar(
  availableLenses: List<LensOption>,
  selectedLens: LensOption,
  currentZoom: Float,
  sceneOptimizerEnabled: Boolean,
  onLensSelected: (LensOption) -> Unit,
  onToggleSceneOptimizer: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp),
    contentAlignment = Alignment.Center
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      // Zoom Capsule Pill
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(24.dp))
          .background(Color(0x6618181A))
          .padding(horizontal = 6.dp, vertical = 4.dp)
          .testTag("zoom_capsule_pill")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          availableLenses.forEach { lens ->
            val isSelected = (currentZoom - lens.zoomRatio).let { kotlin.math.abs(it) < 0.15f }
            val bgColor by animateColorAsState(
              targetValue = if (isSelected) Color(0xCC262628) else Color.Transparent,
              label = "lens_bg"
            )

            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(bgColor)
                .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null
                ) {
                  onLensSelected(lens)
                }
                .testTag("lens_button_${lens.label}"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = lens.label,
                color = CameraTextWhite,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
              )
            }
          }
        }
      }

      // Space between capsule and sparkle button
      Box(modifier = Modifier.padding(start = 24.dp)) {
        // Scene optimizer / retouch circular button
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (sceneOptimizerEnabled) Color(0x9928282A) else Color(0x6618181A))
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null
            ) {
              onToggleSceneOptimizer()
            }
            .testTag("scene_optimizer_button"),
          contentAlignment = Alignment.Center
        ) {
          SparklesIcon(
            tint = if (sceneOptimizerEnabled) CameraYellow else CameraTextWhite,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
  }
}

@Composable
fun SparklesIcon(tint: Color, modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val cx = size.width / 2f
    val cy = size.height / 2f

    // Main diamond sparkle in center
    drawSparkle(Offset(cx, cy), size.minDimension * 0.35f, tint)

    // Secondary sparkle top-right
    drawSparkle(Offset(cx + size.width * 0.28f, cy - size.height * 0.25f), size.minDimension * 0.18f, tint)

    // Small sparkle bottom-left
    drawSparkle(Offset(cx - size.width * 0.25f, cy + size.height * 0.22f), size.minDimension * 0.14f, tint)
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSparkle(
  center: Offset,
  radius: Float,
  color: Color
) {
  val path = androidx.compose.ui.graphics.Path().apply {
    moveTo(center.x, center.y - radius)
    cubicTo(
      center.x, center.y - radius * 0.2f,
      center.x + radius * 0.2f, center.y,
      center.x + radius, center.y
    )
    cubicTo(
      center.x + radius * 0.2f, center.y,
      center.x, center.y + radius * 0.2f,
      center.x, center.y + radius
    )
    cubicTo(
      center.x, center.y + radius * 0.2f,
      center.x - radius * 0.2f, center.y,
      center.x - radius, center.y
    )
    cubicTo(
      center.x - radius * 0.2f, center.y,
      center.x, center.y - radius * 0.2f,
      center.x, center.y - radius
    )
    close()
  }
  drawPath(path, color = color)
}
