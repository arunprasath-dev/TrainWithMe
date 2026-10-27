package com.trainwithme.core.model.profile

import com.trainwithme.core.model.Fixtures
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class ProfileValidatorTest {

    @Test
    fun `a complete profile is valid`() {
        val issues = ProfileValidator.validate(Fixtures.profile())
        assertTrue(issues.isEmpty(), "unexpected issues: $issues")
    }

    @Test
    fun `out of range values are errors attached to their field`() {
        val profile = Fixtures.profile(age = 10, weightKg = 500.0, days = 9, minutes = 5)
        val fields = ProfileValidator.firstErrorByField(profile).keys
        assertTrue(ProfileField.AGE in fields)
        assertTrue(ProfileField.WEIGHT in fields)
        assertTrue(ProfileField.DAYS_PER_WEEK in fields)
        assertTrue(ProfileField.SESSION_MINUTES in fields)
        assertFalse(ProfileValidator.isValid(profile))
    }

    @Test
    fun `disclaimer is required`() {
        val profile = Fixtures.profile().copy(agreedToDisclaimer = false)
        assertEquals(IssueCode.DISCLAIMER_REQUIRED, ProfileValidator.firstErrorByField(profile)[ProfileField.DISCLAIMER]?.code)
    }

    @Test
    fun `blank name is an error`() {
        val profile = Fixtures.profile().copy(name = "   ")
        assertEquals(ProfileField.NAME, ProfileValidator.errors(profile).first().field)
    }

    @Test
    fun `underweight user targeting fat loss is warned but not blocked`() {
        val profile = Fixtures.profile(weightKg = 52.0, heightCm = 180.0, goal = Goal.LOSE_FAT, bodyFat = null)
        val warnings = ProfileValidator.warnings(profile)
        assertTrue(warnings.any { it.code == IssueCode.UNDERWEIGHT_WITH_FAT_LOSS })
        assertTrue(ProfileValidator.isValid(profile))
    }

    @Test
    fun `minors receive a supervision warning`() {
        val warnings = ProfileValidator.warnings(Fixtures.profile(age = 16, bodyFat = null))
        assertTrue(warnings.any { it.code == IssueCode.MINOR_NEEDS_SUPERVISION })
    }

    @Test
    fun `waist to height risk is flagged`() {
        val profile = Fixtures.profile(circumferences = Circumference(waistCm = 110.0))
        assertTrue(ProfileValidator.warnings(profile).any { it.code == IssueCode.WAIST_TO_HEIGHT_RISK })
    }

    @Test
    fun `aggressive goal rate is flagged`() {
        val profile = Fixtures.profile(goal = Goal.LOSE_FAT, rate = -1.6)
        assertTrue(ProfileValidator.warnings(profile).any { it.code == IssueCode.AGGRESSIVE_RATE })
    }

    @Test
    fun `body fat outside plausible range is an error`() {
        val profile = Fixtures.profile(bodyFat = 99.0)
        assertEquals(ProfileField.BODY_FAT, ProfileValidator.firstErrorByField(profile)[ProfileField.BODY_FAT]?.field)
    }
}
