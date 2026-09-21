package de.jurihock.voicesmith

import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.core.content.FileProvider
import de.jurihock.voicesmith.etc.Game
import de.jurihock.voicesmith.etc.Log
import de.jurihock.voicesmith.etc.Preferences
import de.jurihock.voicesmith.etc.Vibrator
import de.jurihock.voicesmith.io.AudioDevice
import de.jurihock.voicesmith.io.AudioDevices
import de.jurihock.voicesmith.io.selectChannels
import de.jurihock.voicesmith.io.selectInputDevice
import de.jurihock.voicesmith.io.selectOutputDevice
import de.jurihock.voicesmith.service.AudioServiceActivity
import de.jurihock.voicesmith.ui.IntParameterScreen
import de.jurihock.voicesmith.ui.BigToggleButtonScreen
import de.jurihock.voicesmith.ui.DeviceSelectorScreen
import de.jurihock.voicesmith.ui.MainTheme
import de.jurihock.voicesmith.ui.UI
import java.io.File

class MainActivity : AudioServiceActivity() {

  private val preferences by lazy { Preferences(this) }
  private val devices by lazy { AudioDevices(this) }
  private val game by lazy { Game(this) }
  private val vibrator by lazy { Vibrator(this) }

  private val channels = mutableIntStateOf(1)
  private val delay = mutableIntStateOf(0)
  private val pitch = mutableIntStateOf(0)
  private val timbre = mutableIntStateOf(0)
  private val liveState = mutableStateOf(false)
  private val recordingState = mutableStateOf(false)
  private val effectsExpanded = mutableStateOf(false)
  private val inputDevice = mutableStateOf("DEFAULT")
  private val outputDevice = mutableStateOf("DEFAULT")

  private fun sync() {
    channels.intValue = preferences.channels
    delay.intValue = preferences.delay
    pitch.intValue = preferences.pitch
    timbre.intValue = preferences.timbre
    inputDevice.value = selectedDeviceName(devices.inputs, preferences.input)
    outputDevice.value = selectedDeviceName(devices.outputs, preferences.output)
  }

  private fun selectedDeviceName(devices: List<AudioDevice>, id: Int): String {
    return devices.firstOrNull { it.id == id }?.name
      ?: devices.firstOrNull { it.id == 0 }?.name
      ?: "DEFAULT"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    try {
      val flags = PackageManager.PackageInfoFlags.of(0)
      val info = packageManager.getPackageInfo(packageName, flags)
      val version = info.versionName
      title = getString(R.string.title_with_version).format(version)
    } catch (exception: NameNotFoundException) {
      Log.e("Unable to determine the package version!", exception)
    }
    try {
      setContent {
        MainTheme {
          val audioActive = liveState.value || recordingState.value
          val distortionConfigured = pitch.intValue != 0 || timbre.intValue != 0

          Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
              DeviceSelectorScreen(
                modifier = Modifier.padding(Dp(UI.PADDING)),
                textInput = getString(R.string.input),
                textOutput = getString(R.string.output),
                textMono = getString(R.string.mono),
                textStereo = getString(R.string.stereo),
                inputDevice = inputDevice,
                outputDevice = outputDevice,
                channels = channels,
                enabled = !audioActive,
                onSelectInputDevice = { onSelectInputDevice() },
                onSelectOutputDevice = { onSelectOutputDevice() },
                onSelectChannels = { onSelectChannels() })
            },
            bottomBar = {
              Column(modifier = Modifier.padding(Dp(UI.PADDING))) {
                BigToggleButtonScreen(
                  textOn = getString(R.string.start_live_distortion),
                  textOff = getString(R.string.stop),
                  value = liveState,
                  enabled = !recordingState.value,
                  onToggle = { onStartStopLiveAudioService() })
                Spacer(modifier = Modifier.height(Dp(UI.PADDING)))
                BigToggleButtonScreen(
                  textOn = getString(R.string.start_recording),
                  textOff = getString(R.string.stop),
                  value = recordingState,
                  enabled = !liveState.value && (recordingState.value || distortionConfigured),
                  onToggle = { onStartStopRecordingAudioService() })
              }
            }) { padding ->
            Column(modifier = Modifier.padding(padding).padding(Dp(UI.PADDING))) {
              Spacer(modifier = Modifier.weight(1f))

              OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { effectsExpanded.value = !effectsExpanded.value }) {
                Text(
                  text = getString(
                    if (effectsExpanded.value) R.string.effects_hide
                    else R.string.effects_show))
              }

              if (effectsExpanded.value) {
                Spacer(modifier = Modifier.height(Dp(UI.PADDING)))
                IntParameterScreen(
                  name = getString(R.string.delay), unit = getString(R.string.milliseconds), value = delay,
                  min = 0, max = 1000, inc = 50,
                  onChange = {
                    delay.intValue = it
                    preferences.delay = it
                  })
                Spacer(modifier = Modifier.height(Dp(UI.PADDING)))
                IntParameterScreen(
                  name = getString(R.string.pitch), unit = getString(R.string.semitones), value = pitch,
                  min = -12, max = +12, inc = 1,
                  enabled = !recordingState.value,
                  onChange = {
                    pitch.intValue = it
                    preferences.pitch = it
                  })
                Spacer(modifier = Modifier.height(Dp(UI.PADDING)))
                IntParameterScreen(
                  name = getString(R.string.timbre), unit = getString(R.string.semitones), value = timbre,
                  min = -12, max = +12, inc = 1,
                  enabled = !recordingState.value,
                  onChange = {
                    timbre.intValue = it
                    preferences.timbre = it
                  })
              }

              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    } finally {
      sync()
    }
  }

  override fun onLiveAudioServiceStarted() {
    liveState.value = true
    game.on()
    vibrator.on()
  }

  override fun onLiveAudioServiceStopped() {
    liveState.value = false
    game.off()
    vibrator.off()
  }

  override fun onRecordingAudioServiceStarted() {
    recordingState.value = true
    game.on()
    vibrator.on()
  }

  override fun onRecordingAudioServiceStopped(file: File) {
    recordingState.value = false
    game.off()
    vibrator.off()
    shareRecording(file)
  }

  override fun onAudioServiceFailed() {
    liveState.value = false
    recordingState.value = false
    game.off()
    vibrator.error()
  }

  private fun shareRecording(file: File) {
    try {
      val uri = FileProvider.getUriForFile(
        this,
        "${packageName}.files",
        file)

      val share = Intent(Intent.ACTION_SEND).apply {
        type = "audio/mpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newUri(contentResolver, file.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

      startActivity(Intent.createChooser(
        share,
        getString(R.string.share_recording)))
    } catch (exception: Throwable) {
      Log.e("Unable to share MP3 recording!", exception)
      Toast.makeText(
        this,
        getString(R.string.share_recording_failed),
        Toast.LENGTH_LONG).show()
    }
  }

  private fun onSelectInputDevice() {
    devices.selectInputDevice(preferences.input) {
      preferences.input = it
      inputDevice.value = selectedDeviceName(devices.inputs, it)
    }
  }

  private fun onSelectOutputDevice() {
    devices.selectOutputDevice(preferences.output) {
      preferences.output = it
      outputDevice.value = selectedDeviceName(devices.outputs, it)
    }
  }

  private fun onSelectChannels() {
    devices.selectChannels(preferences.channels) {
      channels.intValue = it
      preferences.channels = it
    }
  }

}
