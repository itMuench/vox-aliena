package de.jurihock.voicesmith.service

import de.jurihock.voicesmith.etc.RecordingFilenameCharacters
import java.util.Random
import kotlin.math.max
import kotlin.math.min

internal fun generateRecordingFilenameBase(
  random: Random,
  dynamicLength: Boolean,
  fixedLength: Int,
  minLength: Int,
  maxLength: Int,
  characters: RecordingFilenameCharacters
): String {
  val fixed = fixedLength.coerceIn(MIN_FILENAME_LENGTH, MAX_FILENAME_LENGTH)
  val firstBound = minLength.coerceIn(MIN_FILENAME_LENGTH, MAX_FILENAME_LENGTH)
  val secondBound = maxLength.coerceIn(MIN_FILENAME_LENGTH, MAX_FILENAME_LENGTH)
  val lower = min(firstBound, secondBound)
  val upper = max(firstBound, secondBound)

  val length =
    if (dynamicLength) lower + random.nextInt(upper - lower + 1)
    else fixed

  val alphabet = when (characters) {
    RecordingFilenameCharacters.NUMBERS -> NUMBERS
    RecordingFilenameCharacters.LETTERS -> LETTERS
    RecordingFilenameCharacters.ALPHANUMERIC -> LETTERS + NUMBERS
  }

  return buildString(length) {
    repeat(length) {
      append(alphabet[random.nextInt(alphabet.length)])
    }
  }
}

internal const val MIN_FILENAME_LENGTH = 10
internal const val MAX_FILENAME_LENGTH = 30

private const val NUMBERS = "0123456789"
private const val LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
