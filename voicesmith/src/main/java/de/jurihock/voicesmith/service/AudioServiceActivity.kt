package de.jurihock.voicesmith.service

import android.app.AlertDialog
import android.content.ComponentName
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import de.jurihock.voicesmith.R
import de.jurihock.voicesmith.etc.Log
import java.io.File

abstract class AudioServiceActivity : ComponentActivity(), ServiceConnection {

  private val permissionToRecordAudio = android.Manifest.permission.RECORD_AUDIO
  private val permissionToPostNotifications = android.Manifest.permission.POST_NOTIFICATIONS

  private val allPermissionsToRequest = arrayOf(
    permissionToRecordAudio,
    permissionToPostNotifications)

  private val permissionRequest = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
      when {
        permissions.getOrDefault(permissionToRecordAudio, false) == true -> {
          Log.i("Record audio permission has been granted")
          enableAudioService()
        }
        permissions.getOrDefault(permissionToRecordAudio, false) == false -> {
          Log.w("Record audio permission has been denied")
          requestedMode = AudioServiceMode.STOPPED
          with(AlertDialog.Builder(this)) {
            setTitle(getString(R.string.permissions_rationale_title))
            setMessage(getString(R.string.permissions_rationale_text))
            setPositiveButton(getString(R.string.permissions_rationale_dismiss)) { dialog, _ ->
              dialog.dismiss()
            }
            setNegativeButton(getString(R.string.permissions_rationale_settings)) { dialog, _ ->
              try {
                val action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.parse("package:${packageName}")
                val intent = android.content.Intent(action, uri)
                startActivity(intent)
              } finally {
                dialog.dismiss()
              }
            }
            create()
            show()
          }
        }
      }
  }

  private var service: AudioService? = null
  private var requestedMode = AudioServiceMode.STOPPED

  private fun enableAudioService() {
    Log.i("Starting audio service")
    startAudioService()
    Log.i("Binding audio service")
    bindAudioService()
  }

  private fun disableAudioService() {
    if (service != null) {
      try {
        Log.i("Unbinding audio service")
        unbindAudioService()
      } catch (exception: Throwable) {
        Log.e(exception)
      }
    }

    try {
      Log.i("Stopping audio service")
      stopAudioService()
    } finally {
      service = null
    }
  }

  protected abstract fun onLiveAudioServiceStarted()
  protected abstract fun onLiveAudioServiceStopped()
  protected abstract fun onRecordingAudioServiceStarted()
  protected abstract fun onRecordingAudioServiceStopped(file: File)
  protected abstract fun onAudioServiceFailed()

  fun onStartStopLiveAudioService() {
    requestAudioMode(AudioServiceMode.LIVE)
  }

  fun onStartStopRecordingAudioService() {
    requestAudioMode(AudioServiceMode.RECORDING)
  }

  private fun requestAudioMode(mode: AudioServiceMode) {
    requestedMode = mode

    val current = service
    if (current == null) {
      permissionRequest.launch(allPermissionsToRequest)
      return
    }

    performAudioMode(current, mode)
  }

  private fun performAudioMode(current: AudioService, requested: AudioServiceMode) {
    try {
      when {
        current.mode == requested -> stopCurrentMode(current)
        current.mode != AudioServiceMode.STOPPED -> Unit
        requested == AudioServiceMode.LIVE -> {
          current.startLive()
          if (current.mode == AudioServiceMode.LIVE) {
            requestedMode = AudioServiceMode.STOPPED
            onLiveAudioServiceStarted()
          }
        }
        requested == AudioServiceMode.RECORDING -> {
          current.startRecording()
          if (current.mode == AudioServiceMode.RECORDING) {
            requestedMode = AudioServiceMode.STOPPED
            onRecordingAudioServiceStarted()
          }
        }
        else -> Unit
      }
    } catch (exception: Throwable) {
      handleAudioServiceFailure(exception)
    }
  }

  private fun stopCurrentMode(current: AudioService) {
    val previousMode = current.mode
    val file = current.stop()

    requestedMode = AudioServiceMode.STOPPED

    when(previousMode) {
      AudioServiceMode.LIVE -> onLiveAudioServiceStopped()
      AudioServiceMode.RECORDING -> {
        if (file != null) {
          onRecordingAudioServiceStopped(file)
        } else {
          onAudioServiceFailed()
        }
      }
      AudioServiceMode.STOPPED -> Unit
    }

    disableAudioService()
  }

  private fun handleAudioServiceFailure(exception: Throwable) {
    requestedMode = AudioServiceMode.STOPPED
    Toast.makeText(this, exception.message, Toast.LENGTH_LONG).show()
    onAudioServiceFailed()
    disableAudioService()
  }

  override fun onDestroy() {
    service?.let {
      try {
        it.stop()
      } catch (exception: Throwable) {
        Log.e(exception)
      }
    }
    disableAudioService()
    super.onDestroy()
  }

  final override fun onServiceConnected(serviceName: ComponentName?, serviceBinder: IBinder?) {
    Log.i("Connecting audio service")

    if (serviceBinder == null) {
      handleAudioServiceFailure(IllegalStateException("Invalid audio service binder!"))
      return
    }

    val binder = serviceBinder as? AudioServiceBinder

    if (binder == null) {
      handleAudioServiceFailure(
        IllegalStateException(
          "Invalid binder type ${serviceBinder::class.java.simpleName} provided by audio service!"))
      return
    }

    service = binder.service

    service?.onServiceError { exception ->
      handleAudioServiceFailure(exception)
    }

    if (requestedMode != AudioServiceMode.STOPPED) {
      performAudioMode(binder.service, requestedMode)
    }
  }

  final override fun onServiceDisconnected(serviceName: ComponentName?) {
    Log.i("Disconnecting audio service")
    service = null
    requestedMode = AudioServiceMode.STOPPED
    onAudioServiceFailed()
  }

}
