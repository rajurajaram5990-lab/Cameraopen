package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CameraFilterEffect
import com.example.ui.theme.CameraDarkOverlay
import com.example.ui.theme.CameraTextWhite
import com.example.ui.theme.CameraYellow

@Composable
fun EffectsFilterSheet(
  isOpen: Boolean,
  currentEffect: CameraFilterEffect,
  onSelectEffect: (CameraFilterEffect) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = isOpen,
    enter = fadeIn() + slideInVertically { it / 2 },
    exit = fadeOut() + slideOutVertically { it / 2 },
    modifier = modifier
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        .background(Color(0xDD141416))
        .padding(vertical = 12.dp)
        .testTag("effects_filter_sheet")
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Filters & Effects",
          color = CameraTextWhite,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold
        )
        IconButton(
          onClick = onClose,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close Filters",
            tint = CameraTextWhite,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      LazyRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
      ) {
        items(CameraFilterEffect.entries) { effect ->
          val isSelected = effect == currentEffect
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
              ) {
                onSelectEffect(effect)
              }
              .testTag("filter_option_${effect.name.lowercase()}")
          ) {
            // Filter Thumbnail Preview Bubble
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(getFilterPreviewColor(effect))
                .border(
                  width = if (isSelected) 2.5.dp else 1.dp,
                  color = if (isSelected) CameraYellow else Color(0x33FFFFFF),
                  shape = CircleShape
                ),
              contentAlignment = Alignment.Center
            ) {
              if (isSelected) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(CameraYellow)
                )
              }
            }

            Text(
              text = effect.title,
              color = if (isSelected) CameraYellow else CameraTextWhite,
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              modifier = Modifier.padding(top = 6.dp)
            )
          }
        }
      }
    }
  }
}

private fun getFilterPreviewColor(effect: CameraFilterEffect): Color {
  return when (effect) {
    CameraFilterEffect.ORIGINAL -> Color(0xFF424242)
    CameraFilterEffect.WARM -> Color(0xFFD48B47)
    CameraFilterEffect.COOL -> Color(0xFF4A84B3)
    CameraFilterEffect.MONO -> Color(0xFF888888)
    CameraFilterEffect.SEPIA -> Color(0xFF9E7C57)
    CameraFilterEffect.NEGATIVE -> Color(0xFF267365)
    CameraFilterEffect.SOLARIZE -> Color(0xFFCC5533)
    CameraFilterEffect.POSTERIZE -> Color(0xFF5D3F6A)
    CameraFilterEffect.AQUA -> Color(0xFF2B8E9B)
  }
}
