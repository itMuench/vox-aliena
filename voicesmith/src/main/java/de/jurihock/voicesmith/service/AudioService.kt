package de.jurihock.voicesmith.service

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import de.jurihock.voicesmith.etc.Log
import de.jurihock.voicesmith.etc.Preferences
import de.jurihock.voicesmith.plug.AudioPlugin
import de.jurihock.voicesmith.plug.TestAudioPlugin
import java.io.File
import java.security.SecureRandom

enum class AudioServiceMode {
  STOPPED,
  LIVE,
  RECORDING
}

class AudioService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {

  private val preferences by lazy { Preferences(this) }

  private var error: ((exception: Throwable) -> Unit)? = null
  private var effectValuesChanged: ((pitch: Double, timbre: Double) -> Unit)? = null
  private var recordingLevelChanged: ((level: Float) -> Unit)? = null
  private var plugin: AudioPlugin? = null
  private var recordingFile: File? = null
  private var currentPitch = 0.0
  private var currentTimbre = 0.0

  private val dynamicHandler = Handler(Looper.getMainLooper())
  private val levelHandler = Handler(Looper.getMainLooper())
  private val pitchWalk = DynamicEffectRandomWalk()
  private val timbreWalk = DynamicEffectRandomWalk()

  private val pitchDynamicUpdate = object : Runnable {
    override fun run() {
      if (mode == AudioServiceMode.STOPPED || !pitchWalk.isEnabled) {
        return
      }

      try {
        pitchWalk.next()?.let { nextPitch ->
          plugin?.set("pitch", nextPitch.toString())
          currentPitch = nextPitch
          notifyEffectValuesChanged()
        }
      } catch (exception: Throwable) {
        onPluginError(exception)
        return
      }

      if (pitchWalk.isEnabled) {
        dynamicHandler.postDelayed(
          this,
          dynamicIntervalMillis(preferences.pitchInterval))
      }
    }
  }

  private val timbreDynamicUpdate = object : Runnable {
    override fun run() {
      if (mode == AudioServiceMode.STOPPED || !timbreWalk.isEnabled) {
        return
      }

      try {
        timbreWalk.next()?.let { nextTimbre ->
          plugin?.set("timbre", nextTimbre.toString())
          currentTimbre = nextTimbre
          notifyEffectValuesChanged()
        }
      } catch (exception: Throwable) {
        onPluginError(exception)
        return
      }

      if (timbreWalk.isEnabled) {
        dynamicHandler.postDelayed(
          this,
          dynamicIntervalMillis(preferences.timbreInterval))
      }
    }
  }

  private val recordingLevelUpdate = object : Runnable {
    override fun run() {
      if (mode != AudioServiceMode.RECORDING) {
        return
      }

      try {
        val level = plugin?.level()?.coerceIn(0f, 1f) ?: 0f
        recordingLevelChanged?.invoke(level)
      } catch (exception: Throwable) {
        onPluginError(exception)
        return
      }

      levelHandler.postDelayed(this, RECORDING_LEVEL_INTERVAL_MS)
    }
  }

  var mode: AudioServiceMode = AudioServiceMode.STOPPED
    private set

  val isStarted: Boolean
    get() = mode != AudioServiceMode.STOPPED && plugin?.isStarted == true

  private fun sync() {
    Log.i("Syncing audio plugin parameters")
    try {
      plugin?.setup(
        preferences.input,
        preferences.output,
        preferences.samplerate,
        preferences.blocksize,
        preferences.channels)
      plugin?.set("delay", preferences.delay.toString())
      plugin?.set("pitch", preferences.pitch.toString())
      plugin?.set("timbre", preferences.timbre.toString())
      currentPitch = preferences.pitch
      currentTimbre = preferences.timbre
    } catch (exception: Throwable) {
      Log.e(exception)
    }
  }

  private fun resetLive() {
    if (mode == AudioServiceMode.RECORDING) {
      Log.i("Audio routing change will be applied to the next recording")
      return
    }

    Log.i("Resetting audio plugin")
    val restart = mode == AudioServiceMode.LIVE

    try {
      stop()
      sync()
      if (restart) {
        startLive()
      }
    } catch (exception: Throwable) {
      onPluginError(exception)
    }
  }

  fun startLive() {
    if (mode != AudioServiceMode.STOPPED) {
      return
    }

    Log.i("Starting live audio plugin")
    prepareDynamicEffects()
    requireNotNull(plugin) { "Audio plugin is unavailable!" }.start()
    mode = AudioServiceMode.LIVE
    scheduleDynamicEffects()
  }

  fun startRecording(): File {
    if (mode != AudioServiceMode.STOPPED) {
      throw IllegalStateException("Audio service is already active!")
    }

    if (preferences.pitch == 0.0 && preferences.timbre == 0.0) {
      throw IllegalStateException(
        "Pitch or Timbre must be non-zero before starting an MP3 recording!")
    }

    val file = createRecordingFile()

    Log.i("Starting MP3 recording to ${file.absolutePath}")
    try {
      prepareDynamicEffects()
      requireNotNull(plugin) { "Audio plugin is unavailable!" }
        .startRecording(file.absolutePath)
      recordingFile = file
      mode = AudioServiceMode.RECORDING
      scheduleDynamicEffects()
      scheduleRecordingLevelUpdates()
      return file
    } catch (exception: Throwable) {
      file.delete()
      throw exception
    }
  }

  fun stop(): File? {
    Log.i("Stopping audio plugin")
    stopDynamicEffects()
    stopRecordingLevelUpdates()

    val finishedRecording =
      if (mode == AudioServiceMode.RECORDING) recordingFile else null

    try {
      plugin?.stop()
    } catch (exception: Throwable) {
      finishedRecording?.delete()
      Log.e(exception)
      throw exception
    } finally {
      mode = AudioServiceMode.STOPPED
      recordingFile = null
    }

    return finishedRecording?.takeIf { it.exists() && it.length() > 0 }
  }

  override fun onBind(intent: Intent?): IBinder = bindAudioService()
  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = startAudioService()

  override fun onCreate() {
    Log.i("Creating audio service")
    try {
      plugin = TestAudioPlugin()
      plugin?.onError { onPluginError(it) }
      sync()
    } catch (exception: Throwable) {
      Log.e(exception)
    }

    Log.i("Subscribing application preferences")
    preferences.register(this)
  }

  override fun onDestroy() {
    Log.i("Unsubscribing application preferences")
    preferences.unregister(this)

    Log.i("Destroying audio service")
    stopDynamicEffects()
    stopRecordingLevelUpdates()
    try {
      plugin?.close()
      plugin = null
    } catch (exception: Throwable) {
      Log.e(exception)
    } finally {
      mode = AudioServiceMode.STOPPED
      recordingFile = null
    }
  }

  override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, name: String?) {
    when(name) {
      "input" -> resetLive()
      "output" -> resetLive()
      "samplerate" -> resetLive()
      "blocksize" -> resetLive()
      "channels" -> resetLive()
      "delay" -> plugin?.set("delay", preferences.delay.toString())
      "pitch" -> if (mode != AudioServiceMode.RECORDING) {
        plugin?.set("pitch", preferences.pitch.toString())
      }
      "timbre" -> if (mode != AudioServiceMode.RECORDING) {
        plugin?.set("timbre", preferences.timbre.toString())
      }
    }
  }

  private fun prepareDynamicEffects() {
    stopDynamicEffects()

    val pitchBase = preferences.pitch
    val timbreBase = preferences.timbre

    pitchWalk.configure(
      baseValue = pitchBase,
      configuredRange = preferences.pitchRange,
      dynamic = preferences.pitchDynamic)
    timbreWalk.configure(
      baseValue = timbreBase,
      configuredRange = preferences.timbreRange,
      dynamic = preferences.timbreDynamic)

    plugin?.set("pitch", pitchBase.toString())
    plugin?.set("timbre", timbreBase.toString())
    currentPitch = pitchBase
    currentTimbre = timbreBase
    notifyEffectValuesChanged()
  }

  private fun notifyEffectValuesChanged() {
    effectValuesChanged?.invoke(currentPitch, currentTimbre)
  }

  private fun scheduleDynamicEffects() {
    if (pitchWalk.isEnabled) {
      dynamicHandler.postDelayed(
        pitchDynamicUpdate,
        dynamicIntervalMillis(preferences.pitchInterval))
    }
    if (timbreWalk.isEnabled) {
      dynamicHandler.postDelayed(
        timbreDynamicUpdate,
        dynamicIntervalMillis(preferences.timbreInterval))
    }
  }

  private fun stopDynamicEffects() {
    dynamicHandler.removeCallbacks(pitchDynamicUpdate)
    dynamicHandler.removeCallbacks(timbreDynamicUpdate)
  }

  fun onEffectValuesChanged(callback: (pitch: Double, timbre: Double) -> Unit) {
    effectValuesChanged = callback
    callback(currentPitch, currentTimbre)
  }

  private fun scheduleRecordingLevelUpdates() {
    levelHandler.removeCallbacks(recordingLevelUpdate)
    recordingLevelChanged?.invoke(0f)
    levelHandler.post(recordingLevelUpdate)
  }

  private fun stopRecordingLevelUpdates() {
    levelHandler.removeCallbacks(recordingLevelUpdate)
    recordingLevelChanged?.invoke(0f)
  }

  fun onRecordingLevelChanged(callback: (level: Float) -> Unit) {
    recordingLevelChanged = callback
    callback(
      if (mode == AudioServiceMode.RECORDING) {
        plugin?.level()?.coerceIn(0f, 1f) ?: 0f
      } else {
        0f
      })
  }

  fun onServiceError(callback: (exception: Throwable) -> Unit) {
    error = callback
  }

  private fun createRecordingFile(): File {
    val external = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
    val directory =
      if (external != null) File(external, "VoxAliena")
      else File(filesDir, "recordings")

    if (!directory.exists() && !directory.mkdirs()) {
      throw IllegalStateException("Unable to create recording directory!")
    }

    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    val random = SecureRandom().apply {
      setSeed(System.currentTimeMillis())
    }

    var file: File
    do {
      val name = buildString {
        repeat(12) {
          append(alphabet[random.nextInt(alphabet.length)])
        }
      }
      file = File(directory, "${name}.mp3")
    } while (file.exists())

    return file
  }

  private fun onPluginError(exception: Throwable) {
    stopDynamicEffects()
    stopRecordingLevelUpdates()
    try {
      plugin?.stop()
    } catch (stopException: Throwable) {
      Log.e(stopException)
    } finally {
      if (mode == AudioServiceMode.RECORDING) {
        recordingFile?.delete()
      }
      mode = AudioServiceMode.STOPPED
      recordingFile = null
      error?.invoke(exception)
    }
  }


  private companion object {
    const val RECORDING_LEVEL_INTERVAL_MS = 50L
  }

}
