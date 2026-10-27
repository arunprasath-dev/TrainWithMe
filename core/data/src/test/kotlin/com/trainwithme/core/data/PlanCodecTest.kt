package com.trainwithme.core.data

import com.trainwithme.core.data.repository.PlanCodec
import com.trainwithme.core.model.plan.PlanGenerationRequest
import com.trainwithme.core.model.plan.PlanGenerationResult
import com.trainwithme.core.model.plan.PlanGenerator
import com.trainwithme.core.model.plan.TrainingPlan
import com.trainwithme.core.model.profile.ActivityLevel
import com.trainwithme.core.model.profile.EquipmentAccess
import com.trainwithme.core.model.profile.ExperienceLevel
import com.trainwithme.core.model.profile.Goal
import com.trainwithme.core.model.profile.Sex
import com.trainwithme.core.model.profile.UserProfile
import java.io.File
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.junit.Test
import com.trainwithme.core.model.exercise.Exercise
import com.trainwithme.core.model.exercise.InMemoryExerciseLibrary

class PlanCodecTest {

    private val library = InMemoryExerciseLibrary(
        Json { ignoreUnknownKeys = true }
            .decodeFromString<List<Exercise>>(
                File("src/main/assets/exercises.json").readText(),
            ),
    )

    private fun plan(): TrainingPlan {
        val profile = UserProfile(
            name = "Codec",
            sex = Sex.FEMALE,
            age = 29,
            heightCm = 166.0,
            weightKg = 63.0,
            bodyFatPercent = 27.0,
            experience = ExperienceLevel.INTERMEDIATE,
            activityLevel = ActivityLevel.LIGHTLY_ACTIVE,
            goal = Goal.RECOMP,
            equipmentAccess = EquipmentAccess.DUMBBELLS,
            daysPerWeek = 4,
            sessionMinutes = 45,
            agreedToDisclaimer = true,
        )
        val result = PlanGenerator(library, clock = { 42L })
            .generate(PlanGenerationRequest(profile, LocalDate.of(2026, 12, 7), planId = "plan-test"))
        assertTrue(result is PlanGenerationResult.Success)
        return (result as PlanGenerationResult.Success).plan
    }

    @Test
    fun `plan survives a json round trip`() {
        val original = plan()
        val encoded = PlanCodec.encode(original)
        assertTrue(encoded.isNotBlank())
        val decoded = assertNotNull(PlanCodec.decode(encoded))

        assertEquals(original.id, decoded.id)
        assertEquals(original.splitName, decoded.splitName)
        assertEquals(original.nutrition.calories, decoded.nutrition.calories)
        assertEquals(original.weeks.size, decoded.weeks.size)
        assertEquals(original.trainingDays.map { it.id }, decoded.trainingDays.map { it.id })
        assertEquals(
            original.trainingDays.flatMap { day -> day.exercises.map { it.exerciseId to it.prescription } },
            decoded.trainingDays.flatMap { day -> day.exercises.map { it.exerciseId to it.prescription } },
        )
        assertEquals(original.notes, decoded.notes)
    }

    @Test
    fun `corrupt payload returns null instead of throwing`() {
        assertNull(PlanCodec.decode("{ this is not json"))
        assertNull(PlanCodec.decode(""))
    }

    @Test
    fun `payload size stays small enough to store in one column`() {
        val encoded = PlanCodec.encode(plan())
        assertTrue(encoded.length < 200_000, "plan payload is ${encoded.length} bytes")
    }
}
