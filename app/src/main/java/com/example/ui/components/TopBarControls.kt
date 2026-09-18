package com.example.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Timer10
import androidx.compose.material.icons.filled.Timer3
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FlashMode
import com.example.model.TimerSetting
import com.example.ui.theme.CameraTextWhite
import com.example.ui.theme.CameraYellow

@Composable
fun TopBarControls(
  flashMode: FlashMode,
  resolutionText: String,
  timerSetting: TimerSetting,
  isFilterMenuOpen: Boolean,
  onToggleFlash: () -> Unit,
  onCycleTimer: () -> Unit,
  onToggleFilters: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(56.dp)
      .padding(horizontal = 16.dp),
    horizontalArrangement = Arrangement.End,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Flash Toggle
    IconButton(
      onClick = onToggleFlash,
      modifier = Modifier
        .size(48.dp)
        .testTag("flash_toggle_button")
    ) {
      when (flashMode) {
        FlashMode.OFF -> {
          // Lightning with diagonal slash
          Icon(
            imageVector = Icons.Default.FlashOff,
            contentDescription = "Flash Off",
            tint = CameraTextWhite,
            modifier = Modifier.size(24.dp)
          )
        }
        FlashMode.ON -> {
          Icon(
            imageVector = Icons.Default.FlashOn,
            contentDescription = "Flash On",
            tint = CameraYellow,
            modifier = Modifier.size(24.dp)
          )
        }
        FlashMode.AUTO -> {
          Icon(
            imageVector = Icons.Default.FlashAuto,
            contentDescription = "Flash Auto",
            tint = CameraYellow,
            modifier = Modifier.size(24.dp)
          )
        }
      }
    }

    // Resolution Badge (e.g. "12M" in bold white text)
    Box(
      modifier = Modifier
        .padding(horizontal = 12.dp)
        .clip(RoundedCornerShape(6.dp))
        .padding(horizontal = 8.dp, vertical = 4.dp)
        .testTag("resolution_badge"),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = resolutionText,
        color = CameraTextWhite,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
      )
    }

    // Timer Setting
    IconButton(
      onClick = onCycleTimer,
      modifier = Modifier
        .size(48.dp)
        .testTag("timer_toggle_button")
    ) {
      Box(contentAlignment = Alignment.Center) {
        when (timerSetting) {
          TimerSetting.OFF -> {
            // Analog timer clock icon with needle
            TimerClockIcon(tint = CameraTextWhite, modifier = Modifier.size(22.dp))
          }
          TimerSetting.SEC_2 -> {
            Text(
              text = "2s",
              color = CameraYellow,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
          }
          TimerSetting.SEC_5 -> {
            Text(
              text = "5s",
              color = CameraYellow,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
          }
          TimerSetting.SEC_10 -> {
            Text(
              text = "10s",
              color = CameraYellow,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
          }
        }
      }
    }

    // Filter/Effects Button (Three overlapping circles Venn diagram)
    IconButton(
      onClick = onToggleFilters,
      modifier = Modifier
        .size(48.dp)
        .testTag("filters_toggle_button")
    ) {
      VennCirclesFilterIcon(
        tint = if (isFilterMenuOpen) CameraYellow else CameraTextWhite,
        modifier = Modifier.size(22.dp)
      )
    }
  }
}

@Composable
fun TimerClockIcon(tint: Color, modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val radius = size.minDimension / 2f - 2f
    val center = Offset(size.width / 2f, size.height / 2f)

    // Outer circle
    drawCircle(
      color = tint,
      radius = radius,
      center = center,
      style = Stroke(width = 2.dp.toPx())
    )

    // Needle from center to top-left (~10 o'clock position like reference)
    val handLength = radius * 0.65f
    val endX = center.x - handLength * 0.707f
    val endY = center.y - handLength * 0.707f
    drawLine(
      color = tint,
      start = center,
      end = Offset(endX, endY),
      strokeWidth = 2.dp.toPx()
    )

    // Center dot
    drawCircle(
      color = tint,
      radius = 2.dp.toPx(),
      center = center
    )
  }
}

@Composable
fun VennCirclesFilterIcon(tint: Color, modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val strokeWidth = 1.8.dp.toPx()
    val r = size.minDimension * 0.28f
    val cx = size.width / 2f
    val cy = size.height / 2f

    // Top circle
    drawCircle(
      color = tint,
      radius = r,
      center = Offset(cx, cy - r * 0.55f),
      style = Stroke(width = strokeWidth)
    )

    // Bottom left circle
    drawCircle(
      color = tint,
      radius = r,
      center = Offset(cx - r * 0.52f, cy + r * 0.45f),
      style = Stroke(width = strokeWidth)
    )

    // Bottom right circle
    drawCircle(
      color = tint,
      radius = r,
      center = Offset(cx + r * 0.52f, cy + r * 0.45f),
      style = Stroke(width = strokeWidth)
    )
  }
}
