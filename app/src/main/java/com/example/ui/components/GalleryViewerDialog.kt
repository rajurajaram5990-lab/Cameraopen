package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.CapturedItem
import com.example.ui.theme.CameraBlack
import com.example.ui.theme.CameraTextMuted
import com.example.ui.theme.CameraTextWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GalleryViewerDialog(
  item: CapturedItem?,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (item == null) return
  val context = LocalContext.current

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = modifier
        .fillMaxSize()
        .background(CameraBlack)
        .testTag("gallery_viewer_screen")
    ) {
      // Photo / Video View
      AsyncImage(
        model = item.uri,
        contentDescription = item.name,
        contentScale = ContentScale.Fit,
        modifier = Modifier
          .fillMaxSize()
          .align(Alignment.Center)
      )

      // Top Header Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.TopCenter)
          .background(Color(0x99000000))
          .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .testTag("gallery_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back to Camera",
            tint = CameraTextWhite,
            modifier = Modifier.size(22.dp)
          )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = item.name,
            color = CameraTextWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
          )
          val formattedDate = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US).format(Date(item.dateAdded * 1000))
          Text(
            text = formattedDate,
            color = CameraTextMuted,
            fontSize = 11.sp
          )
        }

        // Share Action
        IconButton(
          onClick = {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
              type = if (item.isVideo) "video/*" else "image/*"
              putExtra(Intent.EXTRA_STREAM, item.uri)
              addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
          },
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .testTag("gallery_share_button")
        ) {
          Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Share",
            tint = CameraTextWhite,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
  }
}
