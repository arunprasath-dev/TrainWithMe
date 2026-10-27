package com.trainwithme.core.model.exercise

import com.trainwithme.core.model.plan.PlannedExercise
import com.trainwithme.core.model.plan.SetPrescription
import com.trainwithme.core.model.profile.EquipmentAccess
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.Test

/**
 * Contract for the catalogue entries that the player and the plan generator rely on: the demo-clip
 * resolution chain, equipment gating and the invariants enforced at construction time.
 */
class ExerciseTest {

    private fun exercise(
        videoAsset: String? = null,
        pattern: MovementPattern = MovementPattern.HORIZONTAL_PUSH,
        equipment: Set<Equipment> = setOf(Equipment.BARBELL, Equipment.BENCH),
    ) = Exercise(
        id = "barbell-bench-press",
        name = "Barbell bench press",
        pattern = pattern,
        primaryMuscles = listOf(MuscleGroup.CHEST),
        equipment = equipment,
        difficulty = 3,
        isCompound = true,
        cues = listOf("Brace.", "Press."),
        videoAsset = videoAsset,
    )

    @Test
    fun `without a dedicated clip the pattern clip is used`() {
        val planned = PlannedExercise(
            exerciseId = exercise().id,
            name = exercise().name,
            pattern = MovementPattern.HORIZONTAL_PUSH,
            primaryMuscles = listOf(MuscleGroup.CHEST),
            slotIndex = 0,
            isCompound = true,
            prescription = SetPrescription(
                sets = 4,
                repLow = 5,
                repHigh = 8,
                restSeconds = 150,
                rpeTarget = 8.0,
                tempo = "2-0-1",
            ),
            videoClipId = exercise().videoClipId,
        )
        assertEquals("horizontal_push", planned.videoClipId)
        assertEquals("videos/horizontal_push.mp4", planned.videoAssetPath)
    }

    @Test
    fun `pattern clip ids are lower case pattern names`() {
        MovementPattern.entries.forEach { pattern ->
            assertEquals(pattern.name.lowercase(), pattern.clipId)
        }
    }

    @Test
    fun `a dedicated clip overrides the pattern clip with or without the extension`() {
        assertEquals("bench-press-demo", exercise(videoAsset = "bench-press-demo").videoClipId)
        assertEquals("bench-press-demo", exercise(videoAsset = "bench-press-demo.mp4").videoClipId)
    }

    @Test
    fun `full gym access can perform barbell movements but bodyweight cannot`() {
        val bench = exercise()
        assertTrue(bench.isPerformableWith(EquipmentAccess.FULL_GYM))
        assertFalse(bench.isPerformableWith(EquipmentAccess.BODYWEIGHT))
        assertFalse(bench.isPerformableWith(EquipmentAccess.DUMBBELLS))
    }

    @Test
    fun `construction rejects an unusable exercise`() {
        assertFailsWith<IllegalArgumentException> {
            Exercise(
                id = " ",
                name = "Nameless",
                pattern = MovementPattern.CORE,
                primaryMuscles = listOf(MuscleGroup.CORE),
                equipment = setOf(Equipment.BODYWEIGHT),
                difficulty = 1,
                isCompound = false,
                cues = listOf("Brace."),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            exercise().copy(difficulty = 7)
        }
        assertFailsWith<IllegalArgumentException> {
            exercise().copy(cues = emptyList())
        }
    }
}
