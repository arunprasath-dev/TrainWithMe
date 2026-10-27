package com.trainwithme.core.data.asset

import android.content.Context
import com.trainwithme.core.model.exercise.Exercise
import com.trainwithme.core.model.exercise.ExerciseLibrary
import com.trainwithme.core.model.exercise.InMemoryExerciseLibrary
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Loads the exercise catalogue from `assets/exercises.json` once per process.
 *
 * Content, not code: the catalogue is edited by coaches without touching Kotlin. If the asset is
 * missing or malformed the app degrades to an empty library instead of crashing, and every screen
 * that needs the catalogue shows an error state (docs/PLAN.md §9.5).
 */
@Singleton
class AssetExerciseLibrary @Inject constructor(
    @ApplicationContext private val context: Context,
) : ExerciseLibrary {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val delegate: ExerciseLibrary by lazy { InMemoryExerciseLibrary(load()) }

    override fun all(): List<Exercise> = delegate.all()

    override fun byId(id: String): Exercise? = delegate.byId(id)

    fun thumbnailAssetName(exerciseId: String): String = "$THUMBNAIL_DIR/$exerciseId.jpg"

    private fun load(): List<Exercise> = try {
        val text = context.assets.open(CATALOGUE_ASSET).bufferedReader().use { it.readText() }
        json.decodeFromString<List<Exercise>>(text)
    } catch (t: Throwable) {
        Timber.e(t, "Unable to load $CATALOGUE_ASSET")
        emptyList()
    }

    companion object {
        const val CATALOGUE_ASSET = "exercises.json"
        const val VIDEO_DIR = "videos"
        const val THUMBNAIL_DIR = "thumbnails"
    }
}
