package de.jurihock.voicesmith.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import java.math.BigDecimal
import kotlin.math.roundToInt

@Composable
fun EffectModeSwitchScreen(modifier: Modifier = Modifier,
                           textSlider: String,
                           textManual: String,
                           manualMode: State<Boolean>,
                           enabled: Boolean = true,
                           onChange: (manual: Boolean) -> Unit) {

  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text = textSlider)
    Spacer(modifier = Modifier.width(Dp(UI.PADDING)))
    Switch(
      checked = manualMode.value,
      enabled = enabled,
      onCheckedChange = onChange)
    Spacer(modifier = Modifier.width(Dp(UI.PADDING)))
    Text(text = textManual)
  }
}

@Composable
fun SemitoneSliderScreen(modifier: Modifier = Modifier,
                         name: String,
                         unit: String,
                         value: State<Double>,
                         min: Int = -12,
                         max: Int = 12,
                         enabled: Boolean = true,
                         onChange: (value: Double) -> Unit) {

  val sliderValue = value.value.roundToInt().coerceIn(min, max)

  Column(modifier = modifier.fillMaxWidth()) {
    Text(
      text = "${name} ${formatSigned(sliderValue)}${unit}",
      modifier = Modifier.fillMaxWidth(),
      textAlign = TextAlign.Center
    )
    Slider(
      value = sliderValue.toFloat(),
      valueRange = min.toFloat()..max.toFloat(),
      steps = max - min - 1,
      enabled = enabled,
      onValueChange = {
        val newValue = it.roundToInt().coerceIn(min, max).toDouble()
        if (newValue != value.value) {
          onChange(newValue)
        }
      })
  }
}

@Composable
fun ManualSemitoneScreen(modifier: Modifier = Modifier,
                         name: String,
                         unit: String,
                         value: State<Double>,
                         rangeText: String,
                         min: Double = -12.0,
                         max: Double = 12.0,
                         enabled: Boolean = true,
                         onChange: (value: Double) -> Unit) {

  var text by remember(value.value) {
    mutableStateOf(formatDecimal(value.value))
  }

  val parsed = parseDecimal(text)
  val isError = parsed == null || parsed < min || parsed > max

  OutlinedTextField(
    modifier = modifier.fillMaxWidth(),
    value = text,
    onValueChange = { newText ->
      text = newText
      parseDecimal(newText)
        ?.takeIf { it >= min && it <= max }
        ?.let { newValue ->
          if (newValue != value.value) {
            onChange(newValue)
          }
        }
    },
    enabled = enabled,
    singleLine = true,
    isError = isError,
    label = { Text(text = "${name} (${unit})") },
    supportingText = { Text(text = rangeText) },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
}

private fun parseDecimal(value: String): Double? {
  return value.trim().replace(',', '.').toDoubleOrNull()
}

private fun formatDecimal(value: Double): String {
  return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
}

private fun formatSigned(value: Int): String {
  return if (value > 0) "+${value}" else value.toString()
}
