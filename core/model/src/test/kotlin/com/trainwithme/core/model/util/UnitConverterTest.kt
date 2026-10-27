package com.trainwithme.core.model.util

import com.trainwithme.core.model.profile.Units
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class UnitConverterTest {

    @Test
    fun `kilogram to pound round trip is lossless`() {
        val kg = 82.5
        val lb = UnitConverter.kgToDisplay(kg, Units.LB)
        assertEquals(181.88, lb, 0.01)
        assertEquals(kg, UnitConverter.displayToKg(lb, Units.LB), 0.0001)
    }

    @Test
    fun `kilogram display is identity`() {
        assertEquals(60.0, UnitConverter.kgToDisplay(60.0, Units.KG), 0.0001)
        assertEquals(60.0, UnitConverter.displayToKg(60.0, Units.KG), 0.0001)
    }

    @Test
    fun `formatting drops trailing zeros`() {
        assertEquals("60 kg", UnitConverter.formatWeight(60.0, Units.KG))
        assertEquals("62.5 kg", UnitConverter.formatWeight(62.5, Units.KG))
        assertEquals("137.8 lb", UnitConverter.formatWeight(62.5, Units.LB))
    }

    @Test
    fun `parsing accepts a unit suffix`() {
        assertEquals(60.0, UnitConverter.parseWeight("60 kg", Units.KG)!!, 0.0001)
        assertEquals(45.359237, UnitConverter.parseWeight("100 lb", Units.LB)!!, 0.0001)
        assertNull(UnitConverter.parseWeight("heavy", Units.KG))
    }

    @Test
    fun `plate steps differ by unit system`() {
        assertEquals(2.5, UnitConverter.weightStep(Units.KG), 0.0001)
        assertEquals(5.0, UnitConverter.weightStep(Units.LB), 0.0001)
    }

    @Test
    fun `round to step snaps to the nearest plate`() {
        assertEquals(62.5, UnitConverter.roundToStep(61.4, 2.5), 0.0001)
        assertEquals(60.0, UnitConverter.roundToStep(61.2, 2.5), 0.0001)
    }

    @Test
    fun `length converts to inches`() {
        assertEquals(70.0, UnitConverter.cmToDisplay(177.8, Units.LB), 0.01)
        assertEquals(177.8, UnitConverter.displayToCm(70.0, Units.LB), 0.01)
        assertEquals("177.8 cm", UnitConverter.formatLength(177.8, Units.KG))
    }
}
