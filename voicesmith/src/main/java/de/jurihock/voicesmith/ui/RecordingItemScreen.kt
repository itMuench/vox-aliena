package de.jurihock.voicesmith.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp

@Composable
fun RecordingItemScreen(modifier: Modifier = Modifier,
                        fileName: String,
                        isPlaying: Boolean,
                        textShare: String,
                        textDelete: String,
                        onPlayPause: () -> Unit,
                        onShare: () -> Unit,
                        onDelete: () -> Unit) {

  val menuExpanded = remember { mutableStateOf(false) }

  Card(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(Dp(UI.PADDING / 2)),
      verticalAlignment = Alignment.CenterVertically) {

      IconButton(onClick = onPlayPause) {
        Text(text = if (isPlaying) "⏸" else "▶")
      }

      Text(
        modifier = Modifier.weight(1f),
        text = fileName,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis)

      OutlinedButton(onClick = onShare) {
        Text(text = textShare)
      }

      Box {
        IconButton(onClick = { menuExpanded.value = true }) {
          Text(text = "⋮")
        }

        DropdownMenu(
          expanded = menuExpanded.value,
          onDismissRequest = { menuExpanded.value = false }) {
          DropdownMenuItem(
            text = { Text(textDelete) },
            onClick = {
              menuExpanded.value = false
              onDelete()
            })
        }
      }
    }
  }

}
