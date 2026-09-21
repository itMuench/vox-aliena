package de.jurihock.voicesmith.service

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Environment
import android.os.IBinder
import de.jurihock.voicesmith.etc.Log
import de.jurihock.voicesmith.etc.Preferences
import de.jurihock.voicesmith.plug.AudioPlugin
import de.jurihock.voicesmith.plug.TestAudioPlugin
import java.io.File

enum class AudioServiceMode {
  STOPPED,
  LIVE,
  RECORDING
}

class AudioService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {

  private val preferences by lazy { Preferences(this) }

  private var error: ((exception: Throwable) -> Unit)? = null
  private var plugin: AudioPlugin? = null
  private var recordingFile: File? = null

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
    requireNotNull(plugin) { "Audio plugin is unavailable!" }.start()
    mode = AudioServiceMode.LIVE
  }

  fun startRecording(): File {
    if (mode != AudioServiceMode.STOPPED) {
      throw IllegalStateException("Audio service is already active!")
    }

    val file = createRecordingFile()

    Log.i("Starting MP3 recording to ${file.absolutePath}")
    try {
      requireNotNull(plugin) { "Audio plugin is unavailable!" }
        .startRecording(file.absolutePath)
      recordingFile = file
      mode = AudioServiceMode.RECORDING
      return file
    } catch (exception: Throwable) {
      file.delete()
      throw exception
    }
  }

  fun stop(): File? {
    Log.i("Stopping audio plugin")

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
      "pitch" -> plugin?.set("pitch", preferences.pitch.toString())
      "timbre" -> plugin?.set("timbre", preferences.timbre.toString())
    }
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

    var timestamp = System.currentTimeMillis()
    var file = File(directory, "${timestamp}.mp3")

    while (file.exists()) {
      timestamp += 1
      file = File(directory, "${timestamp}.mp3")
    }

    return file
  }

  private fun onPluginError(exception: Throwable) {
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

}
