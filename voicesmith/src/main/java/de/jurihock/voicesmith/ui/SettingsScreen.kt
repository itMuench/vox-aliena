package de.jurihock.voicesmith.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import de.jurihock.voicesmith.etc.RecordingFilenameCharacters
import kotlin.math.abs

@Composable
fun SettingsScreen(modifier: Modifier = Modifier,
                   title: String,
                   saveText: String,
                   textSlider: String,
                   textManual: String,
                   textStatic: String,
                   textDynamic: String,
                   dynamicRangePrefix: String,
                   dynamicRangeUnavailable: String,
                   delayName: String,
                   milliseconds: String,
                   pitchName: String,
                   pitchRangeName: String,
                   pitchIntervalName: String,
                   timbreName: String,
                   timbreRangeName: String,
                   timbreIntervalName: String,
                   semitones: String,
                   seconds: String,
                   intervalRangeText: String,
                   manualRangeText: String,
                   filenameTitle: String,
                   filenameLengthLabel: String,
                   filenameDynamicLengthLabel: String,
                   filenameMinLengthLabel: String,
                   filenameMaxLengthLabel: String,
                   filenameLengthRangeText: String,
                   filenameCharactersTitle: String,
                   filenameNumbersOnlyLabel: String,
                   filenameLettersOnlyLabel: String,
                   filenameAlphanumericLabel: String,
                   delay: State<Int>,
                   pitch: State<Double>,
                   pitchDynamic: State<Boolean>,
                   pitchRange: State<Double>,
                   pitchInterval: State<Double>,
                   timbre: State<Double>,
                   timbreDynamic: State<Boolean>,
                   timbreRange: State<Double>,
                   timbreInterval: State<Double>,
                   manualMode: State<Boolean>,
                   filenameLength: State<Int>,
                   filenameDynamicLength: State<Boolean>,
                   filenameMinLength: State<Int>,
                   filenameMaxLength: State<Int>,
                   filenameCharacters: State<RecordingFilenameCharacters>,
                   onDelayChange: (value: Int) -> Unit,
                   onPitchChange: (value: Double) -> Unit,
                   onPitchDynamicChange: (dynamic: Boolean) -> Unit,
                   onPitchRangeChange: (value: Double) -> Unit,
                   onPitchIntervalChange: (value: Double) -> Unit,
                   onTimbreChange: (value: Double) -> Unit,
                   onTimbreDynamicChange: (dynamic: Boolean) -> Unit,
                   onTimbreRangeChange: (value: Double) -> Unit,
                   onTimbreIntervalChange: (value: Double) -> Unit,
                   onModeChange: (manual: Boolean) -> Unit,
                   onFilenameLengthChange: (value: Int) -> Unit,
                   onFilenameDynamicLengthChange: (value: Boolean) -> Unit,
                   onFilenameMinLengthChange: (value: Int) -> Unit,
                   onFilenameMaxLengthChange: (value: Int) -> Unit,
                   onFilenameCharactersChange: (value: RecordingFilenameCharacters) -> Unit,
                   onSave: () -> Unit) {

  val pitchMaxRange = (12.0 - abs(pitch.value)).coerceAtLeast(0.0)
  val timbreMaxRange = (12.0 - abs(timbre.value)).coerceAtLeast(0.0)

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
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
    } else {
      SemitoneSliderScreen(
        name = pitchName,
        unit = semitones,
        value = pitch,
        onChange = onPitchChange)
    }

    Spacer(modifier = Modifier.height(Dp(UI.PADDING)))

    DynamicRangeScreen(
      name = pitchRangeName,
      unit = semitones,
      intervalName = pitchIntervalName,
      intervalUnit = seconds,
      intervalRangeText = intervalRangeText,
      textStatic = textStatic,
      textDynamic = textDynamic,
      allowedRangePrefix = dynamicRangePrefix,
      unavailableText = dynamicRangeUnavailable,
      dynamic = pitchDynamic,
      value = pitchRange,
      interval = pitchInterval,
      maxRange = pitchMaxRange,
      onDynamicChange = onPitchDynamicChange,
      onRangeChange = onPitchRangeChange,
      onIntervalChange = onPitchIntervalChange)

    Spacer(modifier = Modifier.height(Dp(UI.PADDING * 2)))
    HorizontalDivider(modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(Dp(UI.PADDING * 2)))

    if (manualMode.value) {
      ManualSemitoneScreen(
        name = timbreName,
        unit = semitones,
        value = timbre,
        rangeText = manualRangeText,
        onChange = onTimbreChange)
    } else {
      SemitoneSliderScreen(
        name = timbreName,
        unit = semitones,
        value = timbre,
        onChange = onTimbreChange)
    }

    Spacer(modifier = Modifier.height(Dp(UI.PADDING)))

    DynamicRangeScreen(
      name = timbreRangeName,
      unit = semitones,
      intervalName = timbreIntervalName,
      intervalUnit = seconds,
      intervalRangeText = intervalRangeText,
      textStatic = textStatic,
      textDynamic = textDynamic,
      allowedRangePrefix = dynamicRangePrefix,
      unavailableText = dynamicRangeUnavailable,
      dynamic = timbreDynamic,
      value = timbreRange,
      interval = timbreInterval,
      maxRange = timbreMaxRange,
      onDynamicChange = onTimbreDynamicChange,
      onRangeChange = onTimbreRangeChange,
      onIntervalChange = onTimbreIntervalChange)

    Spacer(modifier = Modifier.height(Dp(UI.PADDING * 2)))
    HorizontalDivider(modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(Dp(UI.PADDING * 2)))

    RecordingFilenameSettingsScreen(
      title = filenameTitle,
      lengthLabel = filenameLengthLabel,
      dynamicLengthLabel = filenameDynamicLengthLabel,
      minLengthLabel = filenameMinLengthLabel,
      maxLengthLabel = filenameMaxLengthLabel,
      lengthRangeText = filenameLengthRangeText,
      charactersTitle = filenameCharactersTitle,
      numbersOnlyLabel = filenameNumbersOnlyLabel,
      lettersOnlyLabel = filenameLettersOnlyLabel,
      alphanumericLabel = filenameAlphanumericLabel,
      length = filenameLength,
      dynamicLength = filenameDynamicLength,
      minLength = filenameMinLength,
      maxLength = filenameMaxLength,
      characters = filenameCharacters,
      onLengthChange = onFilenameLengthChange,
      onDynamicLengthChange = onFilenameDynamicLengthChange,
      onMinLengthChange = onFilenameMinLengthChange,
      onMaxLengthChange = onFilenameMaxLengthChange,
      onCharactersChange = onFilenameCharactersChange)

    Spacer(modifier = Modifier.height(Dp(UI.PADDING * 2)))

    Button(
      modifier = Modifier.fillMaxWidth(),
      onClick = onSave
    ) {
      Text(text = saveText)
    }
  }
}
