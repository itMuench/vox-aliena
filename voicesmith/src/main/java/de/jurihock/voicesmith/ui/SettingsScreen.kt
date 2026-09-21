package de.jurihock.voicesmith.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
fun SettingsScreen(modifier: Modifier = Modifier,
                   title: String,
                   saveText: String,
                   textSlider: String,
                   textManual: String,
                   delayName: String,
                   milliseconds: String,
                   pitchName: String,
                   timbreName: String,
                   semitones: String,
                   manualRangeText: String,
                   delay: State<Int>,
                   pitch: State<Double>,
                   timbre: State<Double>,
                   manualMode: State<Boolean>,
                   onDelayChange: (value: Int) -> Unit,
                   onPitchChange: (value: Double) -> Unit,
                   onTimbreChange: (value: Double) -> Unit,
                   onModeChange: (manual: Boolean) -> Unit,
                   onSave: () -> Unit) {

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(Dp(UI.PADDING))
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.headlineMedium)

    Spacer(modifier = Modifier.height(Dp(UI.PADDING * 2)))

    EffectModeSwitchScreen(
      textSlider = textSlider,
      textManual = textManual,
      manualMode = manualMode,
      onChange = onModeChange)

    Spacer(modifier = Modifier.height(Dp(UI.PADDING)))

    IntParameterScreen(
      name = delayName,
      unit = milliseconds,
      value = delay,
      min = 0,
      max = 1000,
      inc = 50,
      onChange = onDelayChange)

    Spacer(modifier = Modifier.height(Dp(UI.PADDING)))

    if (manualMode.value) {
      ManualSemitoneScreen(
        name = pitchName,
        unit = semitones,
        value = pitch,
        rangeText = manualRangeText,
        onChange = onPitchChange)

      Spacer(modifier = Modifier.height(Dp(UI.PADDING)))

      ManualSemitoneScreen(
        name = timbreName,
        unit = semitones,
        value = timbre,
        rangeText = manualRangeText,
        onChange = onTimbreChange)
    } else {
      SemitoneSliderScreen(
        name = pitchName,
        unit = semitones,
        value = pitch,
        onChange = onPitchChange)

      Spacer(modifier = Modifier.height(Dp(UI.PADDING)))

      SemitoneSliderScreen(
        name = timbreName,
        unit = semitones,
        value = timbre,
        onChange = onTimbreChange)
    }

    Spacer(modifier = Modifier.height(Dp(UI.PADDING * 2)))

    Button(
      modifier = Modifier.fillMaxWidth(),
      onClick = onSave
    ) {
      Text(text = saveText)
    }
  }
}
