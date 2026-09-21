package de.jurihock.voicesmith.io

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioDevicesTest {

  @Test
  fun unknownDeviceTypeFallsBackToProductName() {
    val name = formatAudioDeviceName(
      id = 42,
      type = 28,
      productName = "Vendor echo reference",
      address = "ignored",
      types = mapOf(1 to "Built-in earpiece"))

    assertEquals("VENDOR ECHO REFERENCE #42", name)
  }

  @Test
  fun knownDeviceTypeUsesFrameworkNameAndAddress() {
    val name = formatAudioDeviceName(
      id = 7,
      type = 1,
      productName = "Phone speaker",
      address = "front",
      types = mapOf(1 to "Built-in earpiece"))

    assertEquals("BUILT-IN EARPIECE FRONT", name)
  }

}
