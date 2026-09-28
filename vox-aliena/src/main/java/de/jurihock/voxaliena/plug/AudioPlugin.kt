package de.jurihock.voxaliena.plug

import de.jurihock.voxaliena.etc.Log
import de.jurihock.voxaliena.io.onError
import de.jurihock.voxaliena.io.toAudioEventCode
import de.jurihock.voxaliena.io.toLogPriority
import de.jurihock.voxaliena.jna.JnaCallback
import de.jurihock.voxaliena.jna.JnaPointerByReference
import de.jurihock.voxaliena.jna.JnaResultByReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class AudioPlugin(val name: String) : AutoCloseable, JnaCallback {

  private val crs = CoroutineScope(Dispatchers.Main)
  private val jna = AudioPluginFactory()
  private val ref = JnaPointerByReference()
  private val res = JnaResultByReference()

  private var error: ((exception: AudioPluginException) -> Unit)? = null
  private var state: Boolean = false

  val isStarted: Boolean
    get() = state and ref.isNotNull

  init {
    res.result { res ->
      jna.voxaliena_plugin_open(name, this, ref, res)
    }.onFailure { throw it }
  }

  fun setup(input: Int, output: Int, samplerate: Int, blocksize: Int, channels: Int) {
    res.result { res ->
      jna.voxaliena_plugin_setup(
        input, output, samplerate, blocksize, channels,
        ref, res)
    }.onFailure { throw it }
  }

  fun set(param: String, value: String) {
    res.result { res ->
      jna.voxaliena_plugin_set(
        param, value,
        ref, res)
    }.onFailure { throw it }
  }

  fun start() {
    res.result { res ->
      jna.voxaliena_plugin_start(ref, res)
    }.onSuccess {
      state = true
    }.onFailure {
      state = false
    }.onFailure { throw it }
  }

  fun startRecording(path: String) {
    res.result { res ->
      jna.voxaliena_plugin_start_recording(path, ref, res)
    }.onSuccess {
      state = true
    }.onFailure {
      state = false
    }.onFailure { throw it }
  }

  fun level(): Float {
    return res.result { res ->
      jna.voxaliena_plugin_level(ref, res)
    }.getOrThrow()
  }

  fun stop() {
    res.result { res ->
      jna.voxaliena_plugin_stop(ref, res)
    }.also {
      state = false
    }.onFailure { throw it }
  }

  override fun close() {
    res.result { res ->
      jna.voxaliena_plugin_close(ref, res)
    }.also {
      state = false
    }.onFailure { throw it }
  }

  override fun callback(code: Int, data: String) {
    crs.launch {
      code.toAudioEventCode?.let { event ->
        Log.p(event.toLogPriority, "${name}: ${event} ${data}")
        event.onError {
          res.result { res ->
            jna.voxaliena_plugin_stop(ref, res)
          }.also {
            state = false
          }.also {
            error?.invoke(AudioPluginException(event, data))
          }.onFailure { throw it }
        }
      }
    }
  }

  fun onError(callback: (exception: AudioPluginException) -> Unit) {
    error = callback
  }

}
