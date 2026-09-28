package de.jurihock.voxaliena.jna

import com.sun.jna.Callback

interface JnaCallback : Callback {

  fun callback(code: Int, data: String)

}
