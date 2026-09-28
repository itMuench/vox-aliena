package de.jurihock.voxaliena.service

import android.os.Binder

class AudioServiceBinder(val service: AudioService) : Binder()
