package com.trainwithme.core.model.util

import com.trainwithme.core.model.profile.Units
import kotlin.math.round
import java.util.Locale

/**
 * Unit conversion for display only. Everything is persisted in kilograms and centimetres
 * (docs/PLAN.md §10) — this object is the single conversion boundary.
 */
object UnitConverter {

    const val KG_PER_LB = 0.45359237
    const val CM_PER_INCH = 2.54
    private const val LB_PER_KG = 1.0 / KG_PER_LB

    fun kgToDisplay(kg: Double, units: Units): Double = when (units) {
        Units.KG -> kg
        Units.LB -> kg * LB_PER_KG
    }

    fun displayToKg(value: Double, units: Units): Double = when (units) {
        Units.KG -> value
        Units.LB -> value * KG_PER_LB
    }

    fun cmToDisplay(cm: Double, units: Units): Double = when (units) {
        Units.KG -> cm
        Units.LB -> cm / CM_PER_INCH
    }

    fun displayToCm(value: Double, units: Units): Double = when (units) {
        Units.KG -> value
        Units.LB -> value * CM_PER_INCH
    }

    /** Weight plates are 2.5 kg / 5 lb apart in most gyms; that is the natural input step. */
    fun weightStep(units: Units): Double = when (units) {
        Units.KG -> 2.5
        Units.LB -> 5.0
    }

    /** Rounds a *display* weight to the nearest plate step (default 1 kg / 2 lb fine steps). */
    fun roundToStep(displayValue: Double, step: Double): Double =
        if (step <= 0) round(displayValue * 10) / 10 else round(displayValue / step) * step

    fun formatWeight(kg: Double, units: Units, withUnit: Boolean = true): String {
        val value = kgToDisplay(kg, units)
        val text = if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.1f", value)
        }
        return if (withUnit) "$text ${unitLabel(units)}" else text
    }

    fun formatLength(cm: Double, units: Units): String {
        val value = cmToDisplay(cm, units)
        val text = String.format(Locale.US, "%.1f", value)
        return if (units == Units.KG) "$text cm" else "$text in"
    }

    fun unitLabel(units: Units): String = when (units) {
        Units.KG -> "kg"
        Units.LB -> "lb"
    }

    fun parseWeight(text: String, units: Units): Double? {
        val cleaned = text.trim().replace(',', '.').removeSuffix(unitLabel(units)).trim()
        val value = cleaned.toDoubleOrNull() ?: return null
        return displayToKg(value, units)
    }
}
