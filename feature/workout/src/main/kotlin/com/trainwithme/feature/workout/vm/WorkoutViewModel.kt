package com.trainwithme.feature.workout.vm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trainwithme.core.data.asset.AssetExerciseLibrary
import com.trainwithme.core.data.repository.PlanRepository
import com.trainwithme.core.data.repository.ProgressRepository
import com.trainwithme.core.data.repository.SettingsRepository
import com.trainwithme.core.data.repository.WorkoutRepository
import com.trainwithme.core.data.util.TimeProvider
import com.trainwithme.core.model.exercise.Exercise
import com.trainwithme.core.model.plan.PlanDay
import com.trainwithme.core.model.plan.PlannedExercise
import com.trainwithme.core.model.plan.PrescriptionRules
import com.trainwithme.core.model.plan.TrainingPlan
import com.trainwithme.core.model.profile.Units
import com.trainwithme.core.model.training.LoadSuggestion
import com.trainwithme.core.model.training.PersonalRecordDelta
import com.trainwithme.core.model.training.ProgressionEngine
import com.trainwithme.core.model.training.SessionSet
import com.trainwithme.core.model.training.SessionStatus
import com.trainwithme.core.model.training.SessionSummary
import com.trainwithme.core.model.training.WorkoutSession
import com.trainwithme.core.model.util.UnitConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.round
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Which surface of the session flow is visible. */
enum class SessionPhase { OVERVIEW, PLAYING, SUMMARY }

data class DraftSet(
    val weightKg: Double = 0.0,
    val reps: Int = 0,
    val rpe: Double? = null,
) {
    val isLoggable: Boolean get() = reps > 0 && weightKg >= 0.0
}

data class RestState(
    val endsAtMillis: Long,
    val totalSeconds: Int,
    val remainingSeconds: Int,
)

data class ExerciseProgress(
    val planned: PlannedExercise,
    val exercise: Exercise?,
    val loggedSets: List<SessionSet>,
    val suggestion: LoadSuggestion?,
    val isSwapped: Boolean = false,
) {
    val completedWorkingSets: Int get() = loggedSets.count { !it.isWarmup }
    val targetSets: Int get() = planned.prescription.sets
    val isDone: Boolean get() = completedWorkingSets >= targetSets
    val nextSetIndex: Int get() = loggedSets.filter { !it.isWarmup }.size + 1
}

data class WorkoutUiState(
    val isLoading: Boolean = true,
    val phase: SessionPhase = SessionPhase.OVERVIEW,
    val session: WorkoutSession? = null,
    val plan: TrainingPlan? = null,
    val day: PlanDay? = null,
    val exercises: List<ExerciseProgress> = emptyList(),
    val currentIndex: Int = 0,
    val draft: DraftSet = DraftSet(),
    val rest: RestState? = null,
    val units: Units = Units.KG,
    val keepScreenOn: Boolean = true,
    val restTimerSoundEnabled: Boolean = true,
    val restCompletionCount: Int = 0,
    val summary: SessionSummary? = null,
    val message: String? = null,
    val warmupChecked: Set<Int> = emptySet(),
    val error: String? = null,
    val sessionAlreadyCompleted: Boolean = false,
    val sessionMilestone: String? = null,
) {
    val current: ExerciseProgress? get() = exercises.getOrNull(currentIndex)
    val totalWorkingSetsLogged: Int get() = exercises.sumOf { it.completedWorkingSets }
    val totalPlannedSets: Int get() = exercises.sumOf { it.targetSets }
    val volumeKg: Double
        get() = exercises.sumOf { progress ->
            progress.loggedSets.filter { !it.isWarmup }.sumOf { it.weightKg * it.reps }
        }
    val allExercisesDone: Boolean get() = exercises.isNotEmpty() && exercises.all { it.isDone }
    val displayWeight: Double get() = UnitConverter.kgToDisplay(draft.weightKg, units)
    val weightStep: Double get() = if (units == Units.KG) 2.5 else 5.0
    val displayedVolume: Double get() = UnitConverter.kgToDisplay(volumeKg, units)
}

sealed interface WorkoutEffect {
    data object SessionFinished : WorkoutEffect
    data object ExitRequested : WorkoutEffect
}

/**
 * The guided session. A single ViewModel owns the whole flow (overview → player → summary) because
 * the session is one transaction that must not be corrupted by partial navigation.
 *
 * Durability (docs/PLAN.md §9.3, risk R4):
 *  * each logged set is persisted immediately;
 *  * the rest timer is anchored to an absolute timestamp, so rotation/backgrounding cannot desync it;
 *  * session id, phase and current exercise survive process death via [SavedStateHandle].
 */
@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val workoutRepository: WorkoutRepository,
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository,
    private val exerciseLibrary: AssetExerciseLibrary,
    private val timeProvider: TimeProvider,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val dayId: String = savedStateHandle.get<String>(ARG_DAY_ID).orEmpty()

    private val sessionIdState = savedStateHandle.getStateFlow<String?>(ARG_SESSION_ID, null)
    private val phaseState = savedStateHandle.getStateFlow(ARG_PHASE, SessionPhase.OVERVIEW.name)
    private val indexState = savedStateHandle.getStateFlow(ARG_EXERCISE_INDEX, 0)

    private val local = MutableStateFlow(LocalState())
    private val swaps = MutableStateFlow<Map<Int, String>>(emptyMap())

    private val _effects = Channel<WorkoutEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** Best e1RM per exercise *before* this session — the baseline for PR detection. */
    private var previousBests: Map<String, Double> = emptyMap()

    private var restTickerActive = false

    @OptIn(ExperimentalCoroutinesApi::class)
    private val sessionFlow: Flow<WorkoutSession?> = sessionIdState.flatMapLatest { id ->
        if (id == null) flowOf(null) else workoutRepository.observeSession(id)
    }

    val uiState: StateFlow<WorkoutUiState> = combine(
        planRepository.observeActivePlan(),
        sessionFlow,
        settingsRepository.observeSettings(),
        combine(local, swaps) { localState, swapState -> localState to swapState },
    ) { plan, session, settings, (localState, swapState) ->
        buildState(
            plan,
            session,
            settings.units,
            settings.keepScreenOn,
            settings.restTimerSoundEnabled,
            localState,
            swapState,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState())

    init {
        restoreRestTimer()
        // Baseline PRs: taken once, before this session's sets exist in the completed history.
        viewModelScope.launch {
            previousBests = progressRepository.observeHistory().first()
                .filter { !it.isWarmup }
                .groupBy { it.exerciseId }
                .mapValues { (_, entries) -> entries.maxOf { ProgressionEngine.epley(it.weightKg, it.reps) } }
        }
        // Resume an in-progress session for this day instead of starting a second one.
        viewModelScope.launch {
            if (sessionIdState.value != null) return@launch
            val plan = planRepository.activePlan() ?: return@launch
            val day = plan.dayById(dayId) ?: return@launch
            val existing = workoutRepository.activeSession()?.takeIf { it.dayId == day.id }
            if (existing != null) {
                savedStateHandle[ARG_SESSION_ID] = existing.id
                savedStateHandle[ARG_PHASE] = SessionPhase.PLAYING.name
            }
        }
        // Progression suggestions for every exercise in the session.
        viewModelScope.launch { loadSuggestions() }
    }

    // -----------------------------------------------------------------------------------------
    // Session lifecycle
    // -----------------------------------------------------------------------------------------

    fun startSession() {
        val state = uiState.value
        val plan = state.plan
        val day = state.day
        if (plan == null || day == null) {
            local.update { it.copy(error = "This day is not part of your active plan.") }
            return
        }
        viewModelScope.launch {
            val session = workoutRepository.startSession(plan, day, timeProvider.nowMillis())
            savedStateHandle[ARG_SESSION_ID] = session.id
            savedStateHandle[ARG_PHASE] = SessionPhase.PLAYING.name
            local.update { it.copy(error = null) }
            loadSuggestions()
        }
    }

    fun toggleWarmupStep(index: Int) = local.update { current ->
        val updated = if (index in current.warmupChecked) {
            current.warmupChecked - index
        } else {
            current.warmupChecked + index
        }
        current.copy(warmupChecked = updated)
    }

    fun finishSession() {
        val session = uiState.value.session ?: return
        viewModelScope.launch {
            completeAndShowMilestone(session.id)
        }
    }

    /** Ends a session early; logged sets are kept and count towards adherence. */
    fun finishEarly() {
        val state = uiState.value
        val session = state.session ?: return
        viewModelScope.launch {
            if (state.totalWorkingSetsLogged > 0) {
                completeAndShowMilestone(session.id)
            } else {
                workoutRepository.abandonSession(session.id, timeProvider.nowMillis())
                _effects.send(WorkoutEffect.ExitRequested)
            }
        }
    }

    private suspend fun completeAndShowMilestone(sessionId: String) {
        val completedBefore = workoutRepository.observeCompletedSessions().first().size
        workoutRepository.completeSession(sessionId, timeProvider.nowMillis())
        local.update {
            it.copy(
                rest = null,
                milestoneMessage = when (completedBefore + 1) {
                    1 -> "First session complete. You showed up and got the work done."
                    5 -> "Five sessions complete. Consistency is taking shape."
                    10 -> "Ten sessions complete. Keep building at your own pace."
                    25 -> "Twenty-five sessions complete. A strong body of work."
                    50 -> "Fifty sessions complete. Your consistency speaks for itself."
                    else -> null
                },
            )
        }
        persistRest(null)
        savedStateHandle[ARG_PHASE] = SessionPhase.SUMMARY.name
    }

    /** Called by the summary screen's "Done" button. */
    fun closeSession() {
        viewModelScope.launch { _effects.send(WorkoutEffect.SessionFinished) }
    }

    fun requestExit() {
        viewModelScope.launch { _effects.send(WorkoutEffect.ExitRequested) }
    }

    fun dismissMessage() = local.update { it.copy(message = null, error = null) }

    // -----------------------------------------------------------------------------------------
    // Set logging
    // -----------------------------------------------------------------------------------------

    fun onWeightTextChange(text: String, units: Units) {
        val parsed = text.replace(',', '.').toDoubleOrNull() ?: return
        val kg = UnitConverter.displayToKg(parsed, units).coerceIn(0.0, 500.0)
        local.update { it.copy(draft = it.draft.copy(weightKg = kg), error = null) }
    }

    fun onRepsChange(reps: Int) = local.update {
        it.copy(draft = it.draft.copy(reps = reps.coerceIn(0, 100)), error = null)
    }

    fun onRpeChange(rpe: Double?) = local.update { current ->
        val next = if (current.draft.rpe == rpe) null else rpe
        current.copy(draft = current.draft.copy(rpe = next))
    }

    fun applySuggestion() {
        val progress = uiState.value.current ?: return
        val suggestion = progress.suggestion ?: return
        local.update {
            it.copy(
                draft = DraftSet(
                    weightKg = suggestion.targetWeightKg ?: it.draft.weightKg,
                    reps = suggestion.targetReps,
                    rpe = null,
                ),
            )
        }
    }

    fun logSet() {
        val state = uiState.value
        val session = state.session ?: return
        val progress = state.current ?: return
        val draft = state.draft
        if (!draft.isLoggable) {
            local.update { it.copy(error = "Enter the weight and reps you completed.") }
            return
        }
        viewModelScope.launch {
            workoutRepository.logSet(
                sessionId = session.id,
                set = SessionSet(
                    exerciseId = progress.planned.exerciseId,
                    exerciseName = progress.planned.name,
                    setIndex = progress.nextSetIndex,
                    weightKg = draft.weightKg,
                    reps = draft.reps,
                    rpe = draft.rpe,
                    targetReps = progress.planned.prescription.repLow,
                ),
            )
            val rest = RestState(
                endsAtMillis = timeProvider.nowMillis() + progress.planned.prescription.restSeconds * 1000L,
                totalSeconds = progress.planned.prescription.restSeconds,
                remainingSeconds = progress.planned.prescription.restSeconds,
            )
            local.update {
                it.copy(
                    error = null,
                    draft = it.draft.copy(reps = progress.planned.prescription.repLow, rpe = null),
                    rest = rest,
                )
            }
            persistRest(rest)
            startRestTicker()
        }
    }

    fun undoLastSet() {
        val state = uiState.value
        val progress = state.current ?: return
        val session = state.session ?: return
        val last = progress.loggedSets.filter { !it.isWarmup }.maxByOrNull { it.setIndex } ?: return
        viewModelScope.launch { workoutRepository.removeSet(session.id, last.id) }
    }

    fun extendRest(seconds: Int) {
        val rest = local.value.rest ?: return
        val extraMillis = seconds.coerceAtLeast(0) * 1000L
        val extended = rest.copy(
            endsAtMillis = rest.endsAtMillis + extraMillis,
            totalSeconds = rest.totalSeconds + seconds.coerceAtLeast(0),
            remainingSeconds = rest.remainingSeconds + seconds.coerceAtLeast(0),
        )
        local.update { it.copy(rest = extended) }
        persistRest(extended)
    }

    fun skipRest() {
        local.update { it.copy(rest = null) }
        persistRest(null)
    }

    // -----------------------------------------------------------------------------------------
    // Exercise navigation
    // -----------------------------------------------------------------------------------------

    fun nextExercise() {
        val state = uiState.value
        if (state.currentIndex >= state.exercises.lastIndex) {
            if (state.totalWorkingSetsLogged > 0) finishSession() else requestExit()
        } else {
            goToExercise(state.currentIndex + 1)
        }
    }

    fun previousExercise() {
        val state = uiState.value
        if (state.currentIndex > 0) goToExercise(state.currentIndex - 1)
    }

    fun goToExercise(index: Int) {
        if (index !in uiState.value.exercises.indices) return
        savedStateHandle[ARG_EXERCISE_INDEX] = index
        val progress = uiState.value.exercises[index]
        local.update {
            it.copy(
                draft = DraftSet(reps = progress.planned.prescription.repLow),
                rest = null,
                message = null,
            )
        }
        persistRest(null)
    }

    /**
     * Substitutes a movement that trains the same pattern and is available with the user's
     * equipment. The swap lasts for this session only (docs/PLAN.md §6.4 D5).
     */
    fun swapExercise() {
        val state = uiState.value
        val progress = state.current ?: return
        val snapshot = state.plan?.snapshot ?: return
        val usedIds = state.exercises.map { it.planned.exerciseId }.toSet()
        val alternatives = exerciseLibrary.all().filter { candidate ->
            candidate.pattern == progress.planned.pattern &&
                candidate.id !in usedIds &&
                candidate.isPerformableWith(snapshot.equipmentAccess) &&
                candidate.difficulty <= PrescriptionRules.difficultyCeiling(snapshot.experience)
        }
        val replacement = alternatives.firstOrNull()
        if (replacement == null) {
            local.update { it.copy(message = "No alternative available for this movement with your equipment.") }
            return
        }
        swaps.update { it + (state.currentIndex to replacement.id) }
        local.update {
            it.copy(
                message = "Swapped to ${replacement.name} for this session.",
                draft = DraftSet(reps = progress.planned.prescription.repLow),
            )
        }
        viewModelScope.launch { loadSuggestions() }
    }

    fun skipExercise() {
        if (uiState.value.exercises.isEmpty()) return
        local.update { it.copy(message = "Exercise skipped — it stays in your plan for next time.") }
        nextExercise()
    }

    // -----------------------------------------------------------------------------------------
    // Internals
    // -----------------------------------------------------------------------------------------

    private suspend fun loadSuggestions() {
        val plan = planRepository.activePlan() ?: return
        val day = plan.dayById(dayId) ?: return
        val week = plan.weekOf(dayId)
        val isDeload = week?.isDeload ?: false
        val swapState = swaps.value
        val suggestions = mutableMapOf<String, LoadSuggestion>()

        day.exercises.forEachIndexed { index, planned ->
            val exerciseId = swapState[index] ?: planned.exerciseId
            val exercise = exerciseLibrary.byId(exerciseId) ?: return@forEachIndexed
            val previous = workoutRepository.previousSets(exerciseId, timeProvider.nowMillis())
            suggestions[exerciseId] = ProgressionEngine.suggest(
                exercise = exercise,
                prescription = planned.prescription,
                previous = previous.map { it.toLoggedSet() },
                isDeloadWeek = isDeload,
            )
        }
        local.update { it.copy(suggestions = suggestions) }
    }

    private fun restoreRestTimer() {
        val endsAtMillis = savedStateHandle.get<Long>(ARG_REST_END_MILLIS) ?: return
        val totalSeconds = savedStateHandle.get<Int>(ARG_REST_TOTAL_SECONDS) ?: 0
        val remaining = ((endsAtMillis - timeProvider.nowMillis() + 999L) / 1000L)
            .toInt()
            .coerceAtLeast(0)
        if (remaining == 0 || totalSeconds <= 0) {
            persistRest(null)
            return
        }
        local.update {
            it.copy(
                rest = RestState(endsAtMillis, totalSeconds, remaining),
            )
        }
        startRestTicker()
    }

    private fun persistRest(rest: RestState?) {
        if (rest == null) {
            savedStateHandle.remove<Long>(ARG_REST_END_MILLIS)
            savedStateHandle.remove<Int>(ARG_REST_TOTAL_SECONDS)
        } else {
            savedStateHandle[ARG_REST_END_MILLIS] = rest.endsAtMillis
            savedStateHandle[ARG_REST_TOTAL_SECONDS] = rest.totalSeconds
        }
    }

    private fun startRestTicker() {
        if (restTickerActive) return
        restTickerActive = true
        viewModelScope.launch {
            while (isActive) {
                delay(TICK_MS)
                val rest = local.value.rest ?: break
                val remaining = ((rest.endsAtMillis - timeProvider.nowMillis()) / 1000L).toInt().coerceAtLeast(0)
                if (remaining <= 0) {
                    local.update { it.copy(rest = null, restCompletionCount = it.restCompletionCount + 1) }
                    persistRest(null)
                    break
                }
                local.update { it.copy(rest = rest.copy(remainingSeconds = remaining)) }
            }
            restTickerActive = false
        }
    }

    private fun buildState(
        plan: TrainingPlan?,
        session: WorkoutSession?,
        units: Units,
        keepScreenOn: Boolean,
        restTimerSoundEnabled: Boolean,
        localState: LocalState,
        swapState: Map<Int, String>,
    ): WorkoutUiState {
        val day = plan?.dayById(dayId)
        val setsByExercise = session?.sets.orEmpty().groupBy { it.exerciseId }

        val exercises = day?.exercises.orEmpty().mapIndexed { index, planned ->
            val overrideId = swapState[index]
            val exercise = (overrideId?.let { exerciseLibrary.byId(it) }) ?: exerciseLibrary.byId(planned.exerciseId)
            val effective = if (overrideId != null && exercise != null) {
                planned.copy(
                    exerciseId = exercise.id,
                    name = exercise.name,
                    videoClipId = exercise.videoClipId,
                    pattern = exercise.pattern,
                    primaryMuscles = exercise.primaryMuscles,
                    isCompound = exercise.isCompound,
                )
            } else {
                planned
            }
            ExerciseProgress(
                planned = effective,
                exercise = exercise,
                loggedSets = setsByExercise[effective.exerciseId].orEmpty().sortedBy { it.setIndex },
                suggestion = localState.suggestions[effective.exerciseId],
                isSwapped = overrideId != null,
            )
        }

        val completed = session?.status == SessionStatus.COMPLETED
        val phase = when {
            completed -> SessionPhase.SUMMARY
            else -> runCatching { SessionPhase.valueOf(phaseState.value) }.getOrDefault(SessionPhase.OVERVIEW)
        }

        return WorkoutUiState(
            isLoading = plan == null,
            phase = phase,
            session = session,
            plan = plan,
            day = day,
            exercises = exercises,
            currentIndex = indexState.value.coerceIn(0, (exercises.size - 1).coerceAtLeast(0)),
            draft = localState.draft,
            rest = localState.rest?.takeIf { it.remainingSeconds > 0 },
            units = units,
            keepScreenOn = keepScreenOn,
            restTimerSoundEnabled = restTimerSoundEnabled,
            restCompletionCount = localState.restCompletionCount,
            summary = if (phase == SessionPhase.SUMMARY) session?.let { buildSummary(it, day) } else null,
            message = localState.message,
            sessionMilestone = localState.milestoneMessage,
            warmupChecked = localState.warmupChecked,
            error = localState.error,
        )
    }

    private fun buildSummary(session: WorkoutSession, day: PlanDay?): SessionSummary {
        val working = session.sets.filter { !it.isWarmup }
        val records = working.groupBy { it.exerciseId }.mapNotNull { (exerciseId, sets) ->
            val best = sets.maxByOrNull { ProgressionEngine.epley(it.weightKg, it.reps) } ?: return@mapNotNull null
            val e1rm = ProgressionEngine.epley(best.weightKg, best.reps)
            val previous = previousBests[exerciseId]
            if (previous != null && previous >= e1rm) return@mapNotNull null
            PersonalRecordDelta(
                exerciseId = exerciseId,
                exerciseName = best.exerciseName,
                weightKg = best.weightKg,
                reps = best.reps,
                estimatedOneRepMaxKg = round(e1rm * 10) / 10,
                previousBestKg = previous,
            )
        }
        return SessionSummary(
            sessionId = session.id,
            title = session.title,
            date = session.date,
            durationMinutes = session.durationMinutes,
            workingSets = working.size,
            totalReps = working.sumOf { it.reps },
            volumeKg = round(working.sumOf { it.weightKg * it.reps } * 10) / 10,
            personalRecords = records,
            plannedSets = day?.totalSets ?: 0,
        )
    }

    private data class LocalState(
        val draft: DraftSet = DraftSet(),
        val rest: RestState? = null,
        val restCompletionCount: Int = 0,
        val suggestions: Map<String, LoadSuggestion> = emptyMap(),
        val message: String? = null,
        val milestoneMessage: String? = null,
        val error: String? = null,
        val warmupChecked: Set<Int> = emptySet(),
    )

    companion object {
        const val ARG_DAY_ID = "dayId"
        const val ARG_SESSION_ID = "sessionId"
        const val ARG_PHASE = "phase"
        const val ARG_EXERCISE_INDEX = "exerciseIndex"
        private const val ARG_REST_END_MILLIS = "restEndMillis"
        private const val ARG_REST_TOTAL_SECONDS = "restTotalSeconds"
        private const val TICK_MS = 500L
    }
}
