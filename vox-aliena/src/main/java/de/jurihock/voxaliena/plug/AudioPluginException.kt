package de.jurihock.voxaliena.plug

import de.jurihock.voxaliena.io.AudioEventCode

class AudioPluginException(val event: AudioEventCode, message: String) : Exception(message)
