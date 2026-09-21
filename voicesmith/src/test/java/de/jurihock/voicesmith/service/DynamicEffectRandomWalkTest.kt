package de.jurihock.voicesmith.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicEffectRandomWalkTest {

  @Test
  fun walksUpToPositiveBoundaryAndThenBackInward() {
    val walk = DynamicEffectRandomWalk { true }
    walk.configure(baseValue = 6.0, configuredRange = 2.0, dynamic = true)

    assertTrue(walk.isEnabled)
    assertEquals(6.0, walk.current(), 1e-9)

    repeat(20) {
      walk.next()
    }

    assertEquals(8.0, walk.current(), 1e-9)
    assertEquals(7.9, walk.next()!!, 1e-9)
  }

  @Test
  fun walksDownToNegativeBoundaryAndThenBackInward() {
    val walk = DynamicEffectRandomWalk { false }
    walk.configure(baseValue = -6.0, configuredRange = 2.0, dynamic = true)

    repeat(20) {
      walk.next()
    }

    assertEquals(-8.0, walk.current(), 1e-9)
    assertEquals(-7.9, walk.next()!!, 1e-9)
  }

  @Test
  fun clampsConfiguredRangeToGlobalTwelveSemitoneBoundary() {
    val walk = DynamicEffectRandomWalk { true }
    walk.configure(baseValue = 11.5, configuredRange = 2.0, dynamic = true)

    repeat(5) {
      walk.next()
    }

    assertEquals(12.0, walk.current(), 1e-9)
    assertEquals(11.9, walk.next()!!, 1e-9)
  }

  @Test
  fun disablesDynamicWalkWhenLessThanOneStepFits() {
    val walk = DynamicEffectRandomWalk { true }
    walk.configure(baseValue = 11.95, configuredRange = 0.05, dynamic = true)

    assertFalse(walk.isEnabled)
    assertEquals(11.95, walk.current(), 1e-9)
    assertNull(walk.next())
  }

  @Test
  fun startsAtConfiguredBaseValue() {
    val walk = DynamicEffectRandomWalk { true }
    walk.configure(baseValue = 6.05, configuredRange = 0.2, dynamic = true)

    assertEquals(6.05, walk.current(), 1e-9)
    assertEquals(6.15, walk.next()!!, 1e-9)
  }

}
