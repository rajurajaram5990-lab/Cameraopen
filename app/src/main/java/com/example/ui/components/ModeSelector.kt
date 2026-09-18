package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CameraMode
import com.example.ui.theme.CameraTextMuted
import com.example.ui.theme.CameraTextWhite

@Composable
fun ModeSelector(
  currentMode: CameraMode,
  onModeSelected: (CameraMode) -> Unit,
  modifier: Modifier = Modifier
) {
  val modes = CameraMode.entries

  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(44.dp)
      .padding(horizontal = 24.dp),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically
  ) {
    modes.forEach { mode ->
      val isSelected = mode == currentMode
      val textColor by animateColorAsState(
        targetValue = if (isSelected) CameraTextWhite else CameraTextMuted,
        label = "mode_text_color"
      )

      Box(
        modifier = Modifier
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
          ) {
            onModeSelected(mode)
          }
          .padding(horizontal = 12.dp, vertical = 8.dp)
          .testTag("mode_${mode.name.lowercase()}"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = mode.title,
          color = textColor,
          fontSize = 13.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          letterSpacing = 0.5.sp
        )
      }
    }
  }
}
