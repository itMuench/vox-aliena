package de.jurihock.voicesmith.etc

import android.content.Context
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.preference.PreferenceManager
import de.jurihock.voicesmith.io.AudioFeatures

enum class RecordingFilenameCharacters {
  NUMBERS,
  LETTERS,
  ALPHANUMERIC
}

class Preferences(context: Context) {

  private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
  private val features by lazy { AudioFeatures(context) }

  private fun getDouble(name: String): Double {
    return when (val value = preferences.all[name]) {
      is Number -> value.toDouble()
      is String -> value.toDoubleOrNull() ?: 0.0
      else -> 0.0
    }
  }

  private fun putDouble(name: String, value: Double) {
    preferences.edit().putString(name, value.toString()).commit()
  }

  var input: Int
    get() { return preferences.getInt(::input.name, 0) }
    set(value) { preferences.edit().putInt(::input.name, value).commit() }

  var output: Int
    get() { return preferences.getInt(::output.name, 0) }
    set(value) { preferences.edit().putInt(::output.name, value).commit() }

  val samplerate: Int
    get() { return features.samplerate }

  val blocksize: Int
    get() { return features.blocksize }

  var channels: Int
    get() { return preferences.getInt(::channels.name, 1) }
    set(value) { preferences.edit().putInt(::channels.name, value).commit() }

  var delay: Int
    get() { return preferences.getInt(::delay.name, 0) }
    set(value) { preferences.edit().putInt(::delay.name, value).commit() }

  var pitch: Double
    get() { return getDouble(::pitch.name) }
    set(value) { putDouble(::pitch.name, value) }

  var timbre: Double
    get() { return getDouble(::timbre.name) }
    set(value) { putDouble(::timbre.name, value) }

  var manualEffects: Boolean
    get() { return preferences.getBoolean(::manualEffects.name, false) }
    set(value) { preferences.edit().putBoolean(::manualEffects.name, value).commit() }

  var pitchDynamic: Boolean
    get() { return preferences.getBoolean(::pitchDynamic.name, false) }
    set(value) { preferences.edit().putBoolean(::pitchDynamic.name, value).commit() }

  var pitchRange: Double
    get() { return getDouble(::pitchRange.name).takeIf { it > 0.0 } ?: 0.1 }
    set(value) { putDouble(::pitchRange.name, value) }

  var pitchInterval: Double
    get() { return getDouble(::pitchInterval.name).takeIf { it in 0.5..5.0 } ?: 1.0 }
    set(value) { putDouble(::pitchInterval.name, value.coerceIn(0.5, 5.0)) }

  var timbreDynamic: Boolean
    get() { return preferences.getBoolean(::timbreDynamic.name, false) }
    set(value) { preferences.edit().putBoolean(::timbreDynamic.name, value).commit() }

  var timbreRange: Double
    get() { return getDouble(::timbreRange.name).takeIf { it > 0.0 } ?: 0.1 }
    set(value) { putDouble(::timbreRange.name, value) }

  var timbreInterval: Double
    get() { return getDouble(::timbreInterval.name).takeIf { it in 0.5..5.0 } ?: 1.0 }
    set(value) { putDouble(::timbreInterval.name, value.coerceIn(0.5, 5.0)) }

  var recordingFilenameLength: Int
    get() { return preferences.getInt(::recordingFilenameLength.name, 12).coerceIn(10, 30) }
    set(value) { preferences.edit().putInt(::recordingFilenameLength.name, value.coerceIn(10, 30)).commit() }

  var recordingFilenameDynamicLength: Boolean
    get() { return preferences.getBoolean(::recordingFilenameDynamicLength.name, false) }
    set(value) { preferences.edit().putBoolean(::recordingFilenameDynamicLength.name, value).commit() }

  var recordingFilenameMinLength: Int
    get() { return preferences.getInt(::recordingFilenameMinLength.name, 10).coerceIn(10, 30) }
    set(value) { preferences.edit().putInt(::recordingFilenameMinLength.name, value.coerceIn(10, 30)).commit() }

  var recordingFilenameMaxLength: Int
    get() { return preferences.getInt(::recordingFilenameMaxLength.name, 30).coerceIn(10, 30) }
    set(value) { preferences.edit().putInt(::recordingFilenameMaxLength.name, value.coerceIn(10, 30)).commit() }

  var recordingFilenameCharacters: RecordingFilenameCharacters
    get() {
      val stored = preferences.getString(
        ::recordingFilenameCharacters.name,
        RecordingFilenameCharacters.ALPHANUMERIC.name)
      return runCatching {
        RecordingFilenameCharacters.valueOf(stored ?: RecordingFilenameCharacters.ALPHANUMERIC.name)
      }.getOrDefault(RecordingFilenameCharacters.ALPHANUMERIC)
    }
    set(value) {
      preferences.edit()
        .putString(::recordingFilenameCharacters.name, value.name)
        .commit()
    }

  var lastShareTarget: String?
    get() { return preferences.getString(::lastShareTarget.name, null) }
    set(value) {
      val editor = preferences.edit()
      if (value == null) {
        editor.remove(::lastShareTarget.name)
      } else {
        editor.putString(::lastShareTarget.name, value)
      }
      editor.commit()
    }

  fun register(listener: OnSharedPreferenceChangeListener) {
    preferences.registerOnSharedPreferenceChangeListener(listener)
  }

  fun unregister(listener: OnSharedPreferenceChangeListener) {
    preferences.unregisterOnSharedPreferenceChangeListener(listener)
  }

}
