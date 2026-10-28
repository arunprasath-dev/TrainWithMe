package com.agon.app.data.util

import java.time.LocalDate
import java.time.ZoneId

/**
 * All "now" reads go through this interface so ViewModels and the engine can be tested with a
 * fixed clock. Nothing in the app calls System.currentTimeMillis() directly outside this file.
 */
interface TimeProvider {
    fun nowMillis(): Long
    fun today(): LocalDate
    fun zone(): ZoneId
}


class SystemTimeProvider () : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())
    override fun zone(): ZoneId = ZoneId.systemDefault()
}
