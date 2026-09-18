package com.example.model

import android.hardware.camera2.CameraMetadata
import android.net.Uri

enum class CameraFacing {
  BACK,
  FRONT
}

enum class FlashMode {
  OFF,
  ON,
  AUTO
}

enum class TimerSetting(val seconds: Int, val label: String) {
  OFF(0, "Off"),
  SEC_2(2, "2s"),
  SEC_5(5, "5s"),
  SEC_10(10, "10s");

  fun next(): TimerSetting {
    val values = entries
    val nextIndex = (ordinal + 1) % values.size
    return values[nextIndex]
  }
}

enum class CameraMode(val title: String) {
  PORTRAIT("PORTRAIT"),
  PHOTO("PHOTO"),
  VIDEO("VIDEO"),
  MORE("MORE")
}

data class LensOption(
  val label: String,
  val zoomRatio: Float,
  val cameraId: String? = null,
  val isPhysicalCamera: Boolean = false
)

data class ResolutionOption(
  val width: Int,
  val height: Int,
  val label: String,
  val megaPixels: String
)

enum class CameraFilterEffect(
  val title: String,
  val camera2EffectMode: Int? = null
) {
  ORIGINAL("Original", CameraMetadata.CONTROL_EFFECT_MODE_OFF),
  WARM("Warm", null),
  COOL("Cool", null),
  MONO("B&W", CameraMetadata.CONTROL_EFFECT_MODE_MONO),
  SEPIA("Sepia", CameraMetadata.CONTROL_EFFECT_MODE_SEPIA),
  NEGATIVE("Negative", CameraMetadata.CONTROL_EFFECT_MODE_NEGATIVE),
  SOLARIZE("Solarize", CameraMetadata.CONTROL_EFFECT_MODE_SOLARIZE),
  POSTERIZE("Posterize", CameraMetadata.CONTROL_EFFECT_MODE_POSTERIZE),
  AQUA("Aqua", CameraMetadata.CONTROL_EFFECT_MODE_AQUA)
}

data class CapturedItem(
  val uri: Uri,
  val name: String,
  val dateAdded: Long,
  val isVideo: Boolean = false,
  val width: Int = 0,
  val height: Int = 0,
  val sizeBytes: Long = 0L
)
