package com.agon.app.data.asset

import android.content.Context
import android.util.Log
import com.agon.app.domain.model.exercise.Exercise
import com.agon.app.domain.model.exercise.ExerciseLibrary
import com.agon.app.domain.model.exercise.InMemoryExerciseLibrary
import kotlinx.serialization.json.Json

/**
 * Loads the exercise catalogue from `assets/exercises.json` once per process.
 *
 * Content, not code: the catalogue is edited by coaches without touching Kotlin. If the asset is
 * missing or malformed the app degrades to an empty library instead of crashing, and every screen
 * that needs the catalogue shows an error state.
 */
class AssetExerciseLibrary(
    context: Context,
) : ExerciseLibrary {

    private val appContext = context.applicationContext

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val delegate: ExerciseLibrary by lazy { InMemoryExerciseLibrary(load()) }

    override fun all(): List<Exercise> = delegate.all()

    override fun byId(id: String): Exercise? = delegate.byId(id)

    fun thumbnailAssetName(exerciseId: String): String = "$THUMBNAIL_DIR/$exerciseId.jpg"

    private fun load(): List<Exercise> = try {
        val text = appContext.assets.open(CATALOGUE_ASSET).bufferedReader().use { it.readText() }
        json.decodeFromString<List<Exercise>>(text)
    } catch (t: Throwable) {
        Log.e("AssetExerciseLibrary", "Unable to load $CATALOGUE_ASSET", t)
        emptyList()
    }

    companion object {
        const val CATALOGUE_ASSET = "exercises.json"
        const val VIDEO_DIR = "videos"
        const val THUMBNAIL_DIR = "thumbnails"
    }
}
