package de.jurihock.voxaliena.service

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.random.Random

internal fun dynamicIntervalMillis(seconds: Double): Long {
  return (seconds.coerceIn(0.5, 5.0) * 1000.0).roundToLong()
}

internal const val MIN_DYNAMIC_ABS_VALUE = 1.0

internal fun normalizeDynamicEffectBase(value: Double): Double {
  if (abs(value) >= MIN_DYNAMIC_ABS_VALUE) {
    return value
  }

  return if (value < 0.0) -MIN_DYNAMIC_ABS_VALUE else MIN_DYNAMIC_ABS_VALUE
}

internal class DynamicEffectRandomWalk(
  private val randomUp: () -> Boolean = { Random.nextBoolean() }
) {

  private var base = 0.0
  private var limitSteps = 0
  private var offsetSteps = 0

  var isEnabled = false
    private set

  fun configure(baseValue: Double, configuredRange: Double, dynamic: Boolean) {
    base = if (dynamic) normalizeDynamicEffectBase(baseValue) else baseValue
    offsetSteps = 0

    val boundaryRange = (12.0 - abs(base)).coerceAtLeast(0.0)
    val effectiveRange = min(configuredRange.coerceAtLeast(0.0), boundaryRange)

    limitSteps = floor((effectiveRange + 1e-9) / STEP).toInt()
    isEnabled = dynamic && limitSteps >= 1
  }

  fun current(): Double = base + offsetSteps * STEP

  fun next(): Double? {
    if (!isEnabled) {
      return null
    }

    val proposedOffset = when {
      offsetSteps <= -limitSteps -> offsetSteps + 1
      offsetSteps >= limitSteps -> offsetSteps - 1
      randomUp() -> offsetSteps + 1
      else -> offsetSteps - 1
    }

    if (isInsideZeroBuffer(proposedOffset)) {
      val direction = proposedOffset - offsetSteps
      val alternateOffset = offsetSteps - direction
      if (alternateOffset in -limitSteps..limitSteps &&
          !isInsideZeroBuffer(alternateOffset)) {
        offsetSteps = alternateOffset
      }
    } else {
      offsetSteps = proposedOffset
    }

    return current()
  }

  private fun isInsideZeroBuffer(offset: Int): Boolean {
    return abs(base + offset * STEP) < MIN_DYNAMIC_ABS_VALUE - VALUE_EPSILON
  }

  private companion object {
    const val STEP = 0.1
    const val VALUE_EPSILON = 1e-9
  }

}
