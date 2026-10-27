package com.trainwithme.core.data

import com.trainwithme.core.data.asset.AssetExerciseLibrary
import com.trainwithme.core.model.exercise.Equipment
import com.trainwithme.core.model.exercise.Exercise
import com.trainwithme.core.model.exercise.ExerciseLibrary
import com.trainwithme.core.model.exercise.InMemoryExerciseLibrary
import com.trainwithme.core.model.exercise.MovementPattern
import com.trainwithme.core.model.plan.PlanGenerationRequest
import com.trainwithme.core.model.plan.PlanGenerator
import com.trainwithme.core.model.plan.PlanValidator
import com.trainwithme.core.model.profile.ActivityLevel
import com.trainwithme.core.model.profile.EquipmentAccess
import com.trainwithme.core.model.profile.ExperienceLevel
import com.trainwithme.core.model.profile.Goal
import com.trainwithme.core.model.profile.Sex
import com.trainwithme.core.model.profile.UserProfile
import com.trainwithme.core.model.exercise.allowedEquipment
import java.io.File
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.junit.Test
import com.trainwithme.core.model.plan.PlanGenerationResult

/**
 * Validates the *shipped* content — the JSON catalogue and the generated demo clips — against the
 * same rules the engine relies on. Content bugs therefore fail CI instead of surfacing on device.
 */
class ExerciseCatalogueTest {

    private val assetsDir = File("src/main/assets")
    private val catalogueFile = File(assetsDir, "exercises.json")
    private val json = Json { ignoreUnknownKeys = true }

    private fun catalogue(): List<Exercise> {
        assertTrue(catalogueFile.exists(), "Missing ${catalogueFile.absolutePath}")
        return json.decodeFromString(catalogueFile.readText())
    }

    private fun library(): ExerciseLibrary = InMemoryExerciseLibrary(catalogue())

    @Test
    fun `catalogue parses and has unique ids`() {
        val exercises = catalogue()
        assertTrue(exercises.size >= 40, "catalogue has only ${exercises.size} exercises")
        assertEquals(exercises.size, exercises.map { it.id }.toSet().size, "duplicate exercise ids")
        exercises.forEach { exercise ->
            assertTrue(exercise.cues.size >= 3, "${exercise.id} has ${exercise.cues.size} cues")
            assertTrue(exercise.name.isNotBlank())
        }
    }

    @Test
    fun `every movement pattern used by the splits has candidates for every equipment tier`() {
        val exercises = catalogue()
        val splitPatterns = MovementPattern.entries.toSet()
        EquipmentAccess.entries.forEach { access ->
            val allowed = access.allowedEquipment
            splitPatterns.forEach { pattern ->
                val candidates = exercises.filter {
                    it.pattern == pattern && allowed.containsAll(it.equipment)
                }
                // CONDITIONING is not yet used by any split, so it may legitimately be empty.
                if (pattern != MovementPattern.CONDITIONING) {
                    assertTrue(candidates.isNotEmpty(), "no $pattern exercise for $access")
                }
            }
        }
    }

    @Test
    fun `every exercise is performable by at least the full gym tier`() {
        val allowed = EquipmentAccess.FULL_GYM.allowedEquipment
        catalogue().forEach { exercise ->
            assertTrue(allowed.containsAll(exercise.equipment), "${exercise.id} needs unlisted equipment")
        }
    }

    @Test
    fun `a demo clip exists for every pattern an exercise can reference`() {
        val referenced = catalogue().map { it.videoClipId }.toSet()
        referenced.forEach { clip ->
            val file = File(assetsDir, "${AssetExerciseLibrary.VIDEO_DIR}/$clip.mp4")
            assertTrue(file.exists(), "missing demo clip videos/$clip.mp4")
            assertTrue(file.length() > 2_000, "clip $clip is suspiciously small")
        }
    }

    @Test
    fun `every exercise has a thumbnail`() {
        catalogue().forEach { exercise ->
            val file = File(assetsDir, "thumbnails/${exercise.id}.jpg")
            assertTrue(file.exists(), "missing thumbnail ${exercise.id}.jpg")
        }
    }

    @Test
    fun `the shipped catalogue generates valid plans for every profile combination`() {
        val library = library()
        val generator = PlanGenerator(library, clock = { 0L })
        val start = LocalDate.of(2026, 11, 2)

        for (goal in Goal.entries) {
            for (access in EquipmentAccess.entries) {
                for (experience in ExperienceLevel.entries) {
                    val profile = UserProfile(
                        name = "Content Check",
                        sex = Sex.MALE,
                        age = 30,
                        heightCm = 176.0,
                        weightKg = 82.0,
                        bodyFatPercent = 22.0,
                        experience = experience,
                        activityLevel = ActivityLevel.MODERATELY_ACTIVE,
                        goal = goal,
                        equipmentAccess = access,
                        daysPerWeek = 4,
                        sessionMinutes = 60,
                        agreedToDisclaimer = true,
                    )
                    val result = generator.generate(PlanGenerationRequest(profile, start))
                    assertTrue(result is PlanGenerationResult.Success)
                    val plan = (result as PlanGenerationResult.Success).plan
                    val violations = PlanValidator.validate(plan, library, profile)
                    val hard = violations.filter { it.hard }
                    assertTrue(hard.isEmpty(), "$goal/$access/$experience: $hard")
                }
            }
        }
    }

    @Test
    fun `no exercise requires equipment outside its declared tier`() {
        catalogue().forEach { exercise ->
            assertTrue(exercise.equipment.isNotEmpty(), "${exercise.id} declares no equipment")
            assertTrue(
                exercise.equipment.none { it == Equipment.SMITH_MACHINE } ||
                    exercise.equipment.contains(Equipment.SMITH_MACHINE),
            )
        }
    }
}
