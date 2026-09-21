package de.jurihock.voicesmith.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.Dp
import de.jurihock.voicesmith.etc.RecordingFilenameCharacters

@Composable
fun RecordingFilenameSettingsScreen(
  modifier: Modifier = Modifier,
  title: String,
  lengthLabel: String,
  dynamicLengthLabel: String,
  minLengthLabel: String,
  maxLengthLabel: String,
  lengthRangeText: String,
  charactersTitle: String,
  numbersOnlyLabel: String,
  lettersOnlyLabel: String,
  alphanumericLabel: String,
  length: State<Int>,
  dynamicLength: State<Boolean>,
  minLength: State<Int>,
  maxLength: State<Int>,
  characters: State<RecordingFilenameCharacters>,
  onLengthChange: (Int) -> Unit,
  onDynamicLengthChange: (Boolean) -> Unit,
  onMinLengthChange: (Int) -> Unit,
  onMaxLengthChange: (Int) -> Unit,
  onCharactersChange: (RecordingFilenameCharacters) -> Unit
) {
  var lengthText by remember(length.value) { mutableStateOf(length.value.toString()) }
  var minLengthText by remember(minLength.value) { mutableStateOf(minLength.value.toString()) }
  var maxLengthText by remember(maxLength.value) { mutableStateOf(maxLength.value.toString()) }

  Column(modifier = modifier.fillMaxWidth()) {
    Text(text = title, style = MaterialTheme.typography.titleLarge)

    Spacer(modifier = Modifier.height(Dp(UI.PADDING)))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(text = dynamicLengthLabel)
      Switch(
        checked = dynamicLength.value,
        onCheckedChange = onDynamicLengthChange)
    }

    Spacer(modifier = Modifier.height(Dp(UI.PADDING)))

    OutlinedTextField(
      modifier = Modifier.fillMaxWidth(),
      value = lengthText,
      onValueChange = { newText ->
        lengthText = newText
        newText.toIntOrNull()
          ?.takeIf { it in 10..30 }
          ?.let(onLengthChange)
      },
      enabled = !dynamicLength.value,
      singleLine = true,
      isError = !dynamicLength.value &&
        (lengthText.toIntOrNull()?.let { it !in 10..30 } ?: true),
      label = { Text(text = lengthLabel) },
      supportingText = { Text(text = lengthRangeText) },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

    Spacer(modifier = Modifier.height(Dp(UI.PADDING)))

    OutlinedTextField(
      modifier = Modifier.fillMaxWidth(),
      value = minLengthText,
      onValueChange = { newText ->
        minLengthText = newText
        newText.toIntOrNull()
          ?.takeIf { it in 10..30 }
          ?.let(onMinLengthChange)
      },
      enabled = dynamicLength.value,
      singleLine = true,
      isError = dynamicLength.value &&
        (minLengthText.toIntOrNull()?.let { it !in 10..30 } ?: true),
      label = { Text(text = minLengthLabel) },
      supportingText = { Text(text = lengthRangeText) },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

    Spacer(modifier = Modifier.height(Dp(UI.PADDING)))

    OutlinedTextField(
      modifier = Modifier.fillMaxWidth(),
      value = maxLengthText,
      onValueChange = { newText ->
        maxLengthText = newText
        newText.toIntOrNull()
          ?.takeIf { it in 10..30 }
          ?.let(onMaxLengthChange)
      },
      enabled = dynamicLength.value,
      singleLine = true,
      isError = dynamicLength.value &&
        (maxLengthText.toIntOrNull()?.let { it !in 10..30 } ?: true),
      label = { Text(text = maxLengthLabel) },
      supportingText = { Text(text = lengthRangeText) },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

    Spacer(modifier = Modifier.height(Dp(UI.PADDING * 2)))

    Text(text = charactersTitle, style = MaterialTheme.typography.titleMedium)

    FilenameCharacterSwitch(
      text = numbersOnlyLabel,
      checked = characters.value == RecordingFilenameCharacters.NUMBERS,
      onChecked = {
        if (it) onCharactersChange(RecordingFilenameCharacters.NUMBERS)
      })

    FilenameCharacterSwitch(
      text = lettersOnlyLabel,
      checked = characters.value == RecordingFilenameCharacters.LETTERS,
      onChecked = {
        if (it) onCharactersChange(RecordingFilenameCharacters.LETTERS)
      })

    FilenameCharacterSwitch(
      text = alphanumericLabel,
      checked = characters.value == RecordingFilenameCharacters.ALPHANUMERIC,
      onChecked = {
        if (it) onCharactersChange(RecordingFilenameCharacters.ALPHANUMERIC)
      })
  }
}

@Composable
private fun FilenameCharacterSwitch(
  text: String,
  checked: Boolean,
  onChecked: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text = text)
    Switch(
      checked = checked,
      onCheckedChange = onChecked)
  }
}
