package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Panorama
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CameraBlack
import com.example.ui.theme.CameraTextMuted
import com.example.ui.theme.CameraTextWhite
import com.example.ui.theme.CameraYellow

data class MoreModeItem(
  val id: String,
  val title: String,
  val icon: ImageVector,
  val description: String
)

@Composable
fun MoreModesSheet(
  isOpen: Boolean,
  onSelectMode: (String) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val modes = remember {
    listOf(
      MoreModeItem("PRO", "PRO", Icons.Default.Tune, "Manual ISO, WB & Focus"),
      MoreModeItem("NIGHT", "NIGHT", Icons.Default.NightsStay, "Low-light enhanced capture"),
      MoreModeItem("PANORAMA", "PANORAMA", Icons.Default.Panorama, "Ultra-wide scenic panorama"),
      MoreModeItem("FOOD", "FOOD", Icons.Default.Restaurant, "Vibrant color & blur bokeh"),
      MoreModeItem("SLOW_MO", "SLOW MO", Icons.Default.SlowMotionVideo, "High frame rate action"),
      MoreModeItem("HYPERLAPSE", "HYPERLAPSE", Icons.Default.FastForward, "Time-lapse motion capture"),
      MoreModeItem("MACRO", "MACRO", Icons.Default.ZoomIn, "Extreme close-up detail")
    )
  }

  AnimatedVisibility(
    visible = isOpen,
    enter = fadeIn() + slideInVertically { it },
    exit = fadeOut() + slideOutVertically { it },
    modifier = modifier
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(CameraBlack.copy(alpha = 0.95f))
        .padding(top = 48.dp, bottom = 48.dp, start = 24.dp, end = 24.dp)
        .testTag("more_modes_overlay")
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Camera Modes",
            color = CameraTextWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )
          IconButton(
            onClick = onClose,
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(Color(0x33FFFFFF))
              .testTag("close_more_modes_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close More Modes",
              tint = CameraTextWhite,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(28.dp))

        LazyVerticalGrid(
          columns = GridCells.Fixed(3),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalArrangement = Arrangement.spacedBy(24.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(modes) { item ->
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x33222224))
                .clickable(
                  interactionSource = remember { MutableInteractionSource() },
                  indication = null
                ) {
                  onSelectMode(item.title)
                }
                .padding(vertical = 18.dp, horizontal = 8.dp)
                .testTag("more_mode_${item.id.lowercase()}")
            ) {
              Box(
                modifier = Modifier
                  .size(52.dp)
                  .clip(CircleShape)
                  .background(Color(0x443A3A3E)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = item.icon,
                  contentDescription = item.title,
                  tint = CameraYellow,
                  modifier = Modifier.size(28.dp)
                )
              }
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = item.title,
                color = CameraTextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = item.description,
                color = CameraTextMuted,
                fontSize = 9.sp,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
              )
            }
          }
        }
      }
    }
  }
}
