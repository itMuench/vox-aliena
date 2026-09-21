package de.jurihock.voicesmith

import android.app.PendingIntent
import android.content.ClipData
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import de.jurihock.voicesmith.ui.MainTheme
import de.jurihock.voicesmith.ui.RecordingItemScreen
import de.jurihock.voicesmith.ui.SettingsScreen
import de.jurihock.voicesmith.ui.UI
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
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
  private val currentPitch = mutableStateOf(0.0)
  private val currentTimbre = mutableStateOf(0.0)
  private val manualEffects = mutableStateOf(false)
  private val settingsOpen = mutableStateOf(false)
  private val draftDelay = mutableIntStateOf(0)
  private val draftPitch = mutableStateOf(0.0)
  private val draftTimbre = mutableStateOf(0.0)
  private val draftManualEffects = mutableStateOf(false)
  private val draftPitchDynamic = mutableStateOf(false)
  private val draftPitchRange = mutableStateOf(0.1)
  private val draftPitchInterval = mutableStateOf(1.0)
  private val draftTimbreDynamic = mutableStateOf(false)
  private val draftTimbreRange = mutableStateOf(0.1)
  private val draftTimbreInterval = mutableStateOf(1.0)
  private val liveState = mutableStateOf(false)
  private val recordingState = mutableStateOf(false)
  private val inputDevice = mutableStateOf("DEFAULT")
  private val outputDevice = mutableStateOf("DEFAULT")
  private val recordings = mutableStateListOf<File>()
  private val playingRecording = mutableStateOf<File?>(null)
  private val quickShareTarget = mutableStateOf<String?>(null)

  private var mediaPlayer: MediaPlayer? = null

  private fun sync() {
    channels.intValue = preferences.channels
    delay.intValue = preferences.delay
    pitch.value = preferences.pitch
    timbre.value = preferences.timbre
    currentPitch.value = preferences.pitch
    currentTimbre.value = preferences.timbre
    manualEffects.value = preferences.manualEffects
    inputDevice.value = selectedDeviceName(devices.inputs, preferences.input)
    outputDevice.value = selectedDeviceName(devices.outputs, preferences.output)
    refreshRecordings()
    refreshQuickShareTarget()
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

          if (settingsOpen.value) {
            BackHandler {
              closeSettings()
            }
            SettingsScreen(
              title = getString(R.string.settings_title),
              saveText = getString(R.string.settings_save),
              textSlider = getString(R.string.effects_mode_slider),
              textManual = getString(R.string.effects_mode_manual),
              textStatic = getString(R.string.effect_mode_static),
              textDynamic = getString(R.string.effect_mode_dynamic),
              dynamicRangePrefix = getString(R.string.dynamic_range_prefix),
              dynamicRangeUnavailable = getString(R.string.dynamic_range_unavailable),
              delayName = getString(R.string.delay),
              milliseconds = getString(R.string.milliseconds),
              pitchName = getString(R.string.pitch),
              pitchRangeName = getString(R.string.pitch_range),
              pitchIntervalName = getString(R.string.pitch_interval),
              timbreName = getString(R.string.timbre),
              timbreRangeName = getString(R.string.timbre_range),
              timbreIntervalName = getString(R.string.timbre_interval),
              semitones = getString(R.string.semitones),
              seconds = getString(R.string.seconds),
              intervalRangeText = getString(R.string.dynamic_interval_range),
              manualRangeText = getString(R.string.effects_manual_range),
              delay = draftDelay,
              pitch = draftPitch,
              pitchDynamic = draftPitchDynamic,
              pitchRange = draftPitchRange,
              pitchInterval = draftPitchInterval,
              timbre = draftTimbre,
              timbreDynamic = draftTimbreDynamic,
              timbreRange = draftTimbreRange,
              timbreInterval = draftTimbreInterval,
              manualMode = draftManualEffects,
              onDelayChange = { draftDelay.intValue = it },
              onPitchChange = { setDraftPitch(it) },
              onPitchDynamicChange = { setDraftPitchDynamic(it) },
              onPitchRangeChange = { setDraftPitchRange(it) },
              onPitchIntervalChange = { setDraftPitchInterval(it) },
              onTimbreChange = { setDraftTimbre(it) },
              onTimbreDynamicChange = { setDraftTimbreDynamic(it) },
              onTimbreRangeChange = { setDraftTimbreRange(it) },
              onTimbreIntervalChange = { setDraftTimbreInterval(it) },
              onModeChange = { onSelectDraftEffectMode(it) },
              onSave = { saveSettings() })
          } else {
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
                        quickShareTarget = quickShareTarget.value,
                        onPlayPause = { toggleRecordingPlayback(file) },
                        onShare = { shareRecording(file) },
                        onQuickShare = { quickShareRecording(file) },
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
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
              ) {
                Text(text = "${getString(R.string.pitch)}: ${formatEffectValue(currentPitch.value)} ${getString(R.string.semitones)}")
                Text(text = "${getString(R.string.timbre)}: ${formatEffectValue(currentTimbre.value)} ${getString(R.string.semitones)}")
              }

              Spacer(modifier = Modifier.height(Dp(UI.PADDING)))

              OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !audioActive,
                onClick = { openSettings() }) {
                Text(text = getString(R.string.settings))
              }

              Spacer(modifier = Modifier.weight(1f))
            }
          }
          }
        }
      }
    } finally {
      sync()
    }
  }

  override fun onResume() {
    super.onResume()
    refreshQuickShareTarget()
  }

  override fun onLiveAudioServiceStarted() {
    liveState.value = true
    game.on()
    vibrator.on()
  }

  override fun onLiveAudioServiceStopped() {
    liveState.value = false
    resetCurrentEffectValues()
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
    resetCurrentEffectValues()
    game.off()
    vibrator.off()

    // Reload from disk after the recorder has fully finalized the MP3. This keeps
    // the UI in sync with the files that actually exist and guarantees that a
    // successfully finished recording is visible immediately.
    refreshRecordings()
  }

  override fun onAudioEffectValuesChanged(pitch: Double, timbre: Double) {
    currentPitch.value = pitch
    currentTimbre.value = timbre
  }

  override fun onAudioServiceFailed() {
    liveState.value = false
    recordingState.value = false
    resetCurrentEffectValues()
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

  private fun createShareIntent(file: File, component: ComponentName? = null): Intent {
    val uri = FileProvider.getUriForFile(
      this,
      "${packageName}.files",
      file)

    return Intent(Intent.ACTION_SEND).apply {
      type = "audio/mpeg"
      putExtra(Intent.EXTRA_STREAM, uri)
      clipData = ClipData.newUri(contentResolver, file.name, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      if (component != null) {
        setComponent(component)
      }
    }
  }

  private fun shareRecording(file: File) {
    try {
      val callback = PendingIntent.getBroadcast(
        this,
        0,
        Intent(this, ShareTargetChosenReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)

      startActivity(Intent.createChooser(
        createShareIntent(file),
        getString(R.string.share_recording),
        callback.intentSender))
    } catch (exception: Throwable) {
      Log.e("Unable to share MP3 recording!", exception)
      Toast.makeText(
        this,
        getString(R.string.share_recording_failed),
        Toast.LENGTH_LONG).show()
    }
  }

  private fun quickShareRecording(file: File) {
    val component = preferences.lastShareTarget
      ?.let { ComponentName.unflattenFromString(it) }

    if (component == null) {
      clearQuickShareTarget()
      shareRecording(file)
      return
    }

    try {
      startActivity(createShareIntent(file, component))
    } catch (exception: Throwable) {
      Log.e("Unable to quick-share MP3 recording!", exception)
      clearQuickShareTarget()
      shareRecording(file)
    }
  }

  private fun refreshQuickShareTarget() {
    val target = preferences.lastShareTarget
    if (target == null) {
      quickShareTarget.value = null
      return
    }

    val component = ComponentName.unflattenFromString(target)
    if (component == null) {
      clearQuickShareTarget()
      return
    }

    try {
      val info = packageManager.getActivityInfo(
        component,
        PackageManager.ComponentInfoFlags.of(0))
      val appLabel = info.applicationInfo.loadLabel(packageManager).toString().trim()
      val activityLabel = info.loadLabel(packageManager).toString().trim()
      quickShareTarget.value = appLabel.ifEmpty { activityLabel }
    } catch (exception: NameNotFoundException) {
      clearQuickShareTarget()
    }
  }

  private fun clearQuickShareTarget() {
    preferences.lastShareTarget = null
    quickShareTarget.value = null
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

  private fun openSettings() {
    draftDelay.intValue = delay.intValue
    draftPitch.value = pitch.value
    draftTimbre.value = timbre.value
    draftManualEffects.value = manualEffects.value
    draftPitchDynamic.value = preferences.pitchDynamic
    draftPitchRange.value = preferences.pitchRange
    draftPitchInterval.value = preferences.pitchInterval
    draftTimbreDynamic.value = preferences.timbreDynamic
    draftTimbreRange.value = preferences.timbreRange
    draftTimbreInterval.value = preferences.timbreInterval
    normalizePitchRange()
    normalizeTimbreRange()
    settingsOpen.value = true
  }

  private fun closeSettings() {
    settingsOpen.value = false
  }

  private fun maxDynamicRange(value: Double): Double {
    return (12.0 - abs(value)).coerceAtLeast(0.0)
  }

  private fun normalizePitchRange() {
    val maxRange = maxDynamicRange(draftPitch.value)
    if (maxRange < 0.1) {
      draftPitchDynamic.value = false
      draftPitchRange.value = 0.1
      return
    }

    draftPitchRange.value = draftPitchRange.value.coerceIn(0.1, maxRange)
  }

  private fun normalizeTimbreRange() {
    val maxRange = maxDynamicRange(draftTimbre.value)
    if (maxRange < 0.1) {
      draftTimbreDynamic.value = false
      draftTimbreRange.value = 0.1
      return
    }

    draftTimbreRange.value = draftTimbreRange.value.coerceIn(0.1, maxRange)
  }

  private fun setDraftPitch(value: Double) {
    draftPitch.value = value
    normalizePitchRange()
  }

  private fun setDraftTimbre(value: Double) {
    draftTimbre.value = value
    normalizeTimbreRange()
  }

  private fun setDraftPitchDynamic(dynamic: Boolean) {
    val maxRange = maxDynamicRange(draftPitch.value)
    draftPitchDynamic.value = dynamic && maxRange >= 0.1
    normalizePitchRange()
  }

  private fun setDraftTimbreDynamic(dynamic: Boolean) {
    val maxRange = maxDynamicRange(draftTimbre.value)
    draftTimbreDynamic.value = dynamic && maxRange >= 0.1
    normalizeTimbreRange()
  }

  private fun setDraftPitchRange(value: Double) {
    val maxRange = maxDynamicRange(draftPitch.value)
    if (draftPitchDynamic.value && maxRange >= 0.1 && value in 0.1..maxRange) {
      draftPitchRange.value = value
    }
  }

  private fun setDraftTimbreRange(value: Double) {
    val maxRange = maxDynamicRange(draftTimbre.value)
    if (draftTimbreDynamic.value && maxRange >= 0.1 && value in 0.1..maxRange) {
      draftTimbreRange.value = value
    }
  }

  private fun setDraftPitchInterval(value: Double) {
    if (draftPitchDynamic.value && value in 1.0..5.0) {
      draftPitchInterval.value = value
    }
  }

  private fun setDraftTimbreInterval(value: Double) {
    if (draftTimbreDynamic.value && value in 1.0..5.0) {
      draftTimbreInterval.value = value
    }
  }

  private fun onSelectDraftEffectMode(manual: Boolean) {
    if (!manual) {
      setDraftPitch(draftPitch.value.roundToInt().coerceIn(-12, 12).toDouble())
      setDraftTimbre(draftTimbre.value.roundToInt().coerceIn(-12, 12).toDouble())
    }

    draftManualEffects.value = manual
  }

  private fun saveSettings() {
    normalizePitchRange()
    normalizeTimbreRange()

    delay.intValue = draftDelay.intValue
    pitch.value = draftPitch.value
    timbre.value = draftTimbre.value
    currentPitch.value = draftPitch.value
    currentTimbre.value = draftTimbre.value
    manualEffects.value = draftManualEffects.value

    preferences.delay = draftDelay.intValue
    preferences.pitch = draftPitch.value
    preferences.timbre = draftTimbre.value
    preferences.manualEffects = draftManualEffects.value
    preferences.pitchDynamic = draftPitchDynamic.value
    preferences.pitchRange = draftPitchRange.value
    preferences.pitchInterval = draftPitchInterval.value.coerceIn(1.0, 5.0)
    preferences.timbreDynamic = draftTimbreDynamic.value
    preferences.timbreRange = draftTimbreRange.value
    preferences.timbreInterval = draftTimbreInterval.value.coerceIn(1.0, 5.0)

    settingsOpen.value = false
  }

  private fun resetCurrentEffectValues() {
    currentPitch.value = pitch.value
    currentTimbre.value = timbre.value
  }

  private fun formatEffectValue(value: Double): String {
    return BigDecimal.valueOf(value)
      .setScale(10, RoundingMode.HALF_UP)
      .stripTrailingZeros()
      .toPlainString()
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
