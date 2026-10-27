package com.trainwithme.core.data.util

import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All "now" reads go through this interface so ViewModels and the engine can be tested with a
 * fixed clock. Nothing in the app calls System.currentTimeMillis() directly outside this file.
 */
interface TimeProvider {
    fun nowMillis(): Long
    fun today(): LocalDate
    fun zone(): ZoneId
}

@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())
    override fun zone(): ZoneId = ZoneId.systemDefault()
}
