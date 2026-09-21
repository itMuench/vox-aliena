package de.jurihock.voicesmith

import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import de.jurihock.voicesmith.ui.BigToggleButtonScreen
import de.jurihock.voicesmith.ui.DeviceSelectorScreen
import de.jurihock.voicesmith.ui.EffectModeSwitchScreen
import de.jurihock.voicesmith.ui.IntParameterScreen
import de.jurihock.voicesmith.ui.MainTheme
import de.jurihock.voicesmith.ui.ManualSemitoneScreen
import de.jurihock.voicesmith.ui.RecordingItemScreen
import de.jurihock.voicesmith.ui.SemitoneSliderScreen
import de.jurihock.voicesmith.ui.UI
import java.io.File
import kotlin.math.roundToInt

class MainActivity : AudioServiceActivity() {

  private val preferences by lazy { Preferences(this) }
  private val devices by lazy { AudioDevices(this) }
  private val game by lazy { Game(this) }
  private val vibrator by lazy { Vibrator(this) }

  private val channels = mutableIntStateOf(1)
  private val delay = mutableIntStateOf(0)
  private val pitch = mutableStateOf(0.0)
  private val timbre = mutableStateOf(0.0)
  private val manualEffects = mutableStateOf(false)
  private val liveState = mutableStateOf(false)
  private val recordingState = mutableStateOf(false)
  private val effectsExpanded = mutableStateOf(false)
  private val inputDevice = mutableStateOf("DEFAULT")
  private val outputDevice = mutableStateOf("DEFAULT")
  private val recordings = mutableStateListOf<File>()
  private val playingRecording = mutableStateOf<File?>(null)

  private var mediaPlayer: MediaPlayer? = null

  private fun sync() {
    channels.intValue = preferences.channels
    delay.intValue = preferences.delay
    pitch.value = preferences.pitch
    timbre.value = preferences.timbre
    manualEffects.value = preferences.manualEffects
    inputDevice.value = selectedDeviceName(devices.inputs, preferences.input)
    outputDevice.value = selectedDeviceName(devices.outputs, preferences.output)
    refreshRecordings()
  }

  private fun selectedDeviceName(devices: List<AudioDevice>, id: Int): String {
    return devices.firstOrNull { it.id == id }?.name
      ?: devices.firstOrNull { it.id == 0 }?.name
      ?: "DEFAULT"
  }

  private fun recordingDirectory(): File {
    val external = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
    return if (external != null) {
      File(external, "VoxAliena")
    } else {
      File(filesDir, "recordings")
    }
  }

  private fun refreshRecordings() {
    val files = recordingDirectory()
      .listFiles { file ->
        file.isFile && file.extension.equals("mp3", ignoreCase = true)
      }
      ?.sortedByDescending { it.lastModified() }
      ?: emptyList()

    recordings.clear()
    recordings.addAll(files)
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
          val distortionConfigured = pitch.value != 0.0 || timbre.value != 0.0

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
                if (recordings.isNotEmpty()) {
                  LazyColumn(
                    modifier = Modifier
                      .fillMaxWidth()
                      .heightIn(
                        min = Dp(UI.PADDING * 5),
                        max = Dp(UI.PADDING * 16)),
                    verticalArrangement = Arrangement.spacedBy(Dp(UI.PADDING))) {
                    items(
                      items = recordings,
                      key = { it.absolutePath }) { file ->
                      RecordingItemScreen(
                        fileName = file.name,
                        isPlaying = playingRecording.value == file,
                        textShare = getString(R.string.recording_share),
                        textDelete = getString(R.string.recording_delete),
                        onPlayPause = { toggleRecordingPlayback(file) },
                        onShare = { shareRecording(file) },
                        onDelete = { deleteRecording(file) })
                    }
                  }
                  Spacer(modifier = Modifier.height(Dp(UI.PADDING)))
                }

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
              OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { effectsExpanded.value = !effectsExpanded.value }) {
                val indicator = if (effectsExpanded.value) "▼" else "▶"
                val label = getString(
                  if (effectsExpanded.value) R.string.effects_hide
                  else R.string.effects_show)
                Text(text = "$indicator $label")
              }

              if (effectsExpanded.value) {
                Spacer(modifier = Modifier.height(Dp(UI.PADDING)))
                EffectModeSwitchScreen(
                  textSlider = getString(R.string.effects_mode_slider),
                  textManual = getString(R.string.effects_mode_manual),
                  manualMode = manualEffects,
                  enabled = !recordingState.value,
                  onChange = { onSelectEffectMode(it) })
                Spacer(modifier = Modifier.height(Dp(UI.PADDING)))
                IntParameterScreen(
                  name = getString(R.string.delay), unit = getString(R.string.milliseconds), value = delay,
                  min = 0, max = 1000, inc = 50,
                  onChange = {
                    delay.intValue = it
                    preferences.delay = it
                  })
                Spacer(modifier = Modifier.height(Dp(UI.PADDING)))

                if (manualEffects.value) {
                  ManualSemitoneScreen(
                    name = getString(R.string.pitch),
                    unit = getString(R.string.semitones),
                    value = pitch,
                    rangeText = getString(R.string.effects_manual_range),
                    enabled = !recordingState.value,
                    onChange = {
                      pitch.value = it
                      preferences.pitch = it
                    })
                  Spacer(modifier = Modifier.height(Dp(UI.PADDING)))
                  ManualSemitoneScreen(
                    name = getString(R.string.timbre),
                    unit = getString(R.string.semitones),
                    value = timbre,
                    rangeText = getString(R.string.effects_manual_range),
                    enabled = !recordingState.value,
                    onChange = {
                      timbre.value = it
                      preferences.timbre = it
                    })
                } else {
                  SemitoneSliderScreen(
                    name = getString(R.string.pitch),
                    unit = getString(R.string.semitones),
                    value = pitch,
                    enabled = !recordingState.value,
                    onChange = {
                      pitch.value = it
                      preferences.pitch = it
                    })
                  Spacer(modifier = Modifier.height(Dp(UI.PADDING)))
                  SemitoneSliderScreen(
                    name = getString(R.string.timbre),
                    unit = getString(R.string.semitones),
                    value = timbre,
                    enabled = !recordingState.value,
                    onChange = {
                      timbre.value = it
                      preferences.timbre = it
                    })
                }
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
    stopRecordingPlayback()
    recordingState.value = true
    game.on()
    vibrator.on()
  }

  override fun onRecordingAudioServiceStopped(file: File) {
    recordingState.value = false
    game.off()
    vibrator.off()

    // Reload from disk after the recorder has fully finalized the MP3. This keeps
    // the UI in sync with the files that actually exist and guarantees that a
    // successfully finished recording is visible immediately.
    refreshRecordings()
  }

  override fun onAudioServiceFailed() {
    liveState.value = false
    recordingState.value = false
    game.off()
    vibrator.error()
  }

  private fun toggleRecordingPlayback(file: File) {
    if (playingRecording.value == file) {
      stopRecordingPlayback()
      return
    }

    stopRecordingPlayback()

    try {
      mediaPlayer = MediaPlayer().apply {
        setDataSource(file.absolutePath)
        setOnCompletionListener {
          stopRecordingPlayback()
        }
        setOnErrorListener { _, _, _ ->
          stopRecordingPlayback()
          Toast.makeText(
            this@MainActivity,
            getString(R.string.play_recording_failed),
            Toast.LENGTH_LONG).show()
          true
        }
        prepare()
        start()
      }
      playingRecording.value = file
    } catch (exception: Throwable) {
      stopRecordingPlayback()
      Log.e("Unable to play MP3 recording!", exception)
      Toast.makeText(
        this,
        getString(R.string.play_recording_failed),
        Toast.LENGTH_LONG).show()
    }
  }

  private fun stopRecordingPlayback() {
    val player = mediaPlayer
    mediaPlayer = null
    playingRecording.value = null

    if (player != null) {
      try {
        player.stop()
      } catch (exception: Throwable) {
        Log.e(exception)
      } finally {
        player.release()
      }
    }
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

  private fun deleteRecording(file: File) {
    if (playingRecording.value == file) {
      stopRecordingPlayback()
    }

    try {
      if (file.exists() && !file.delete()) {
        throw IllegalStateException("Unable to delete MP3 recording!")
      }
      recordings.remove(file)
    } catch (exception: Throwable) {
      Log.e("Unable to delete MP3 recording!", exception)
      Toast.makeText(
        this,
        getString(R.string.delete_recording_failed),
        Toast.LENGTH_LONG).show()
    }
  }

  private fun onSelectEffectMode(manual: Boolean) {
    if (!manual) {
      val sliderPitch = pitch.value.roundToInt().coerceIn(-12, 12).toDouble()
      val sliderTimbre = timbre.value.roundToInt().coerceIn(-12, 12).toDouble()

      pitch.value = sliderPitch
      timbre.value = sliderTimbre
      preferences.pitch = sliderPitch
      preferences.timbre = sliderTimbre
    }

    manualEffects.value = manual
    preferences.manualEffects = manual
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

  override fun onDestroy() {
    stopRecordingPlayback()
    super.onDestroy()
  }

}
