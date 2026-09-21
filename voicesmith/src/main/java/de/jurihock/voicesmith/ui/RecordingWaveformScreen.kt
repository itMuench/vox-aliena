package de.jurihock.voicesmith.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

@Composable
fun RecordingWaveformScreen(modifier: Modifier = Modifier,
                            levels: List<Float>) {

  val waveformColor = MaterialTheme.colorScheme.primary
  val baselineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
  val barsColor = waveformColor.copy(alpha = 0.28f)

  Canvas(modifier = modifier) {
    if (size.width <= 0f || size.height <= 0f) {
      return@Canvas
    }

    val centerY = size.height / 2f
    val halfHeight = size.height * 0.45f
    val baselineWidth = 1.dp.toPx()
    val waveformWidth = 1.5.dp.toPx()
    val barWidth = 1.dp.toPx()

    drawLine(
      color = baselineColor,
      start = Offset(0f, centerY),
      end = Offset(size.width, centerY),
      strokeWidth = baselineWidth)

    if (levels.isEmpty()) {
      return@Canvas
    }

    val step =
      if (levels.size > 1) size.width / (levels.size - 1)
      else size.width

    val upper = Path()
    val lower = Path()

    levels.forEachIndexed { index, rawLevel ->
      // RMS levels are perceptually compressed so normal speech remains visible
      // without making quiet background noise look like a loud signal.
      val level = sqrt(rawLevel.coerceIn(0f, 1f).toDouble()).toFloat()
      val amplitude = level * halfHeight
      val x = if (levels.size > 1) index * step else size.width / 2f

      drawLine(
        color = barsColor,
        start = Offset(x, centerY - amplitude),
        end = Offset(x, centerY + amplitude),
        strokeWidth = barWidth)

      val upperY = centerY - amplitude
      val lowerY = centerY + amplitude

      if (index == 0) {
        upper.moveTo(x, upperY)
        lower.moveTo(x, lowerY)
      } else {
        upper.lineTo(x, upperY)
        lower.lineTo(x, lowerY)
      }
    }

    drawPath(
      path = upper,
      color = waveformColor,
      style = Stroke(width = waveformWidth))
    drawPath(
      path = lower,
      color = waveformColor,
      style = Stroke(width = waveformWidth))
  }

}
