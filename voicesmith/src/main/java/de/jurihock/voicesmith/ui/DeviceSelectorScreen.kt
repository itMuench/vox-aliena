package de.jurihock.voicesmith.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp

@Composable
fun DeviceSelectorScreen(modifier: Modifier = Modifier,
                         textInput: String, textOutput: String,
                         textMono: String, textStereo: String,
                         inputDevice: State<String>,
                         outputDevice: State<String>,
                         channels: State<Int>,
                         enabled: Boolean = true,
                         onSelectInputDevice: () -> Unit,
                         onSelectOutputDevice: () -> Unit,
                         onSelectChannels: () -> Unit) {

  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Bottom
  ) {
    DeviceButton(
      modifier = Modifier.weight(1f),
      deviceName = inputDevice.value,
      buttonText = textInput,
      enabled = enabled,
      onClick = onSelectInputDevice)
    Spacer(modifier = Modifier.width(Dp(UI.PADDING)))
    OutlinedButton(
      modifier = Modifier.weight(0.5f),
      enabled = enabled,
      onClick = onSelectChannels) {
      Text(text = if (channels.value != 2) textMono else textStereo)
    }
    Spacer(modifier = Modifier.width(Dp(UI.PADDING)))
    DeviceButton(
      modifier = Modifier.weight(1f),
      deviceName = outputDevice.value,
      buttonText = textOutput,
      enabled = enabled,
      onClick = onSelectOutputDevice)
  }

}

@Composable
private fun DeviceButton(modifier: Modifier = Modifier,
                         deviceName: String,
                         buttonText: String,
                         enabled: Boolean,
                         onClick: () -> Unit) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      modifier = Modifier.fillMaxWidth(),
      text = deviceName,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center)
    OutlinedButton(
      modifier = Modifier.fillMaxWidth(),
      enabled = enabled,
      onClick = onClick) {
      Text(text = buttonText)
    }
  }
}
