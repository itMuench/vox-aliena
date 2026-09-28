package de.jurihock.voxaliena.plug

import com.sun.jna.Native
import de.jurihock.voxaliena.jna.JnaCallback
import de.jurihock.voxaliena.jna.JnaPointerByReference
import de.jurihock.voxaliena.jna.JnaResultByReference

@Suppress("KotlinJniMissingFunction", "FunctionName")
open class AudioPluginFactory {

  init {
    Native.register(AudioPluginFactory::class.java, "voxaliena")
  }

  external fun voxaliena_plugin_open(name: String, callback: JnaCallback, pointer: JnaPointerByReference, result: JnaResultByReference) : Boolean
  external fun voxaliena_plugin_setup(input: Int, output: Int, samplerate: Int, blocksize: Int, channels: Int, pointer: JnaPointerByReference, result: JnaResultByReference) : Boolean
  external fun voxaliena_plugin_set(param: String, value: String, pointer: JnaPointerByReference, result: JnaResultByReference) : Boolean
  external fun voxaliena_plugin_start(pointer: JnaPointerByReference, result: JnaResultByReference) : Boolean
  external fun voxaliena_plugin_start_recording(path: String, pointer: JnaPointerByReference, result: JnaResultByReference) : Boolean
  external fun voxaliena_plugin_level(pointer: JnaPointerByReference, result: JnaResultByReference) : Float
  external fun voxaliena_plugin_stop(pointer: JnaPointerByReference, result: JnaResultByReference) : Boolean
  external fun voxaliena_plugin_close(pointer: JnaPointerByReference, result: JnaResultByReference) : Boolean

}
