package de.jurihock.voicesmith.service

import de.jurihock.voicesmith.etc.RecordingFilenameCharacters
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingFilenameGeneratorTest {

  @Test
  fun generatesFixedLengthAlphanumericNames() {
    val name = generateRecordingFilenameBase(
      random = Random(1234L),
      dynamicLength = false,
      fixedLength = 12,
      minLength = 10,
      maxLength = 30,
      characters = RecordingFilenameCharacters.ALPHANUMERIC)

    assertEquals(12, name.length)
    assertTrue(name.all { it.isLetterOrDigit() })
  }

  @Test
  fun generatesNumbersOnly() {
    val name = generateRecordingFilenameBase(
      random = Random(1234L),
      dynamicLength = false,
      fixedLength = 18,
      minLength = 10,
      maxLength = 30,
      characters = RecordingFilenameCharacters.NUMBERS)

    assertEquals(18, name.length)
    assertTrue(name.all { it.isDigit() })
  }

  @Test
  fun generatesLettersOnly() {
    val name = generateRecordingFilenameBase(
      random = Random(1234L),
      dynamicLength = false,
      fixedLength = 18,
      minLength = 10,
      maxLength = 30,
      characters = RecordingFilenameCharacters.LETTERS)

    assertEquals(18, name.length)
    assertTrue(name.all { it.isLetter() })
  }

  @Test
  fun dynamicLengthStaysInsideConfiguredInclusiveRange() {
    repeat(100) { seed ->
      val name = generateRecordingFilenameBase(
        random = Random(seed.toLong()),
        dynamicLength = true,
        fixedLength = 12,
        minLength = 12,
        maxLength = 23,
        characters = RecordingFilenameCharacters.ALPHANUMERIC)

      assertTrue(name.length in 12..23)
    }
  }

  @Test
  fun clampsAllLengthsToTenThroughThirty() {
    assertEquals(
      10,
      generateRecordingFilenameBase(
        Random(1L), false, 2, 10, 30, RecordingFilenameCharacters.NUMBERS).length)
    assertEquals(
      30,
      generateRecordingFilenameBase(
        Random(1L), false, 80, 10, 30, RecordingFilenameCharacters.NUMBERS).length)
  }

}
