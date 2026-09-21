package de.jurihock.voicesmith.service

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.random.Random

internal class DynamicEffectRandomWalk(
  private val randomUp: () -> Boolean = { Random.nextBoolean() }
) {

  private var base = 0.0
  private var limitSteps = 0
  private var offsetSteps = 0

  var isEnabled = false
    private set

  fun configure(baseValue: Double, configuredRange: Double, dynamic: Boolean) {
    base = baseValue
    offsetSteps = 0

    val boundaryRange = (12.0 - abs(baseValue)).coerceAtLeast(0.0)
    val effectiveRange = min(configuredRange.coerceAtLeast(0.0), boundaryRange)

    limitSteps = floor((effectiveRange + 1e-9) / STEP).toInt()
    isEnabled = dynamic && limitSteps >= 1
  }

  fun current(): Double = base + offsetSteps * STEP

  fun next(): Double? {
    if (!isEnabled) {
      return null
    }

    offsetSteps = when {
      offsetSteps <= -limitSteps -> offsetSteps + 1
      offsetSteps >= limitSteps -> offsetSteps - 1
      randomUp() -> offsetSteps + 1
      else -> offsetSteps - 1
    }

    return current()
  }

  private companion object {
    const val STEP = 0.1
  }

}
