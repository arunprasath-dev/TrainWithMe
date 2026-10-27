package com.trainwithme.core.model.plan

import com.trainwithme.core.model.Fixtures
import com.trainwithme.core.model.exercise.MovementPattern
import com.trainwithme.core.model.profile.EquipmentAccess
import com.trainwithme.core.model.profile.ExperienceLevel
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test

class SplitCatalogTest {

    @Test
    fun `every frequency from two to six days has a split`() {
        (2..6).forEach { days ->
            val split = SplitCatalog.forDaysPerWeek(days)
            assertEquals(days, split.trainingDayIndices.size, "split for $days days")
            assertEquals(days, split.templates.size)
        }
    }

    @Test
    fun `out of range frequencies clamp to the supported ones`() {
        assertEquals(2, SplitCatalog.forDaysPerWeek(1).trainingDayIndices.size)
        assertEquals(6, SplitCatalog.forDaysPerWeek(9).trainingDayIndices.size)
    }

    @Test
    fun `training days leave room for recovery`() {
        (2..6).forEach { days ->
            val split = SplitCatalog.forDaysPerWeek(days)
            assertTrue(split.trainingDayIndices.size == split.trainingDayIndices.toSet().size)
            assertTrue(split.trainingDayIndices.all { it in 0..6 })
            if (days <= 4) {
                val consecutive = split.trainingDayIndices.zipWithNext().count { (a, b) -> b - a == 1 }
                assertTrue(consecutive <= days / 2, "$days-day split has $consecutive back-to-back days")
            }
        }
    }

    @Test
    fun `every split covers all muscle groups`() {
        val required = setOf(
            MovementPattern.HORIZONTAL_PUSH,
            MovementPattern.HORIZONTAL_PULL,
            MovementPattern.VERTICAL_PUSH,
            MovementPattern.VERTICAL_PULL,
            MovementPattern.SQUAT,
            MovementPattern.HINGE,
            MovementPattern.CORE,
        )
        SplitCatalog.splits.forEach { split ->
            val patterns = split.templates.flatMap { template -> template.slots.map { it.pattern } }.toSet()
            assertTrue(patterns.containsAll(required), "${split.name} is missing ${required - patterns}")
        }
    }

    @Test
    fun `templates always start with a high priority compound slot`() {
        SplitCatalog.splits.forEach { split ->
            split.templates.forEach { template ->
                val first = template.slots.first()
                assertEquals(0, first.priority, "${template.title} first slot is not priority 0")
                assertTrue(first.isCompound, "${template.title} first slot is not a compound")
            }
        }
    }

    @Test
    fun `every slot is performable with every equipment tier`() {
        ExperienceLevel.entries.forEach { experience ->
            EquipmentAccess.entries.forEach { access ->
                val profile = Fixtures.profile(equipment = access, experience = experience)
                val usable = Fixtures.allExercises.filter { it.isPerformableWith(access) }
                SplitCatalog.splits.forEach { split ->
                    split.templates.forEach { template ->
                        template.slots.forEach { slot ->
                            val candidates = usable.filter {
                                it.pattern == slot.pattern &&
                                    it.difficulty <= PrescriptionRules.difficultyCeiling(experience)
                            }
                            assertTrue(
                                candidates.isNotEmpty(),
                                "${template.title}/${slot.pattern} has no candidate for $access/$experience",
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `training day lookup returns the template for that weekday`() {
        val split = SplitCatalog.forDaysPerWeek(3)
        assertNotNull(split.templateFor(split.trainingDayIndices.first()))
        assertEquals(null, split.templateFor(1))
    }
}
