package com.example.game

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.data.AppDatabase
import com.example.data.ArrowModel
import com.example.data.GameProgressEntity
import com.example.data.GridPoint
import com.example.data.LevelDefinition
import com.example.data.LevelGenerator
import com.example.data.LevelRecordEntity
import com.example.data.LevelRepository
import com.example.haptics.HapticManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.TimeZone
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.gameDao()

    val progressFlow: StateFlow<GameProgressEntity?> = dao.getProgressFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val achievementsFlow = dao.getAllAchievementsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val soundManager = SoundManager(application) {
        progressFlow.value?.soundEnabled ?: true
    }

    val hapticManager = HapticManager(application) {
        progressFlow.value?.hapticsEnabled ?: true
    }

    private val _screenState = MutableStateFlow(ScreenState.SPLASH)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private val _currentLevelState = MutableStateFlow<LevelPlayState?>(null)
    val currentLevelState: StateFlow<LevelPlayState?> = _currentLevelState.asStateFlow()

    private val _shards = MutableStateFlow<List<CrystalShard>>(emptyList())
    val shards: StateFlow<List<CrystalShard>> = _shards.asStateFlow()

    private val _lastCompletedLevel = MutableStateFlow<LevelDefinition?>(null)
    val lastCompletedLevel: StateFlow<LevelDefinition?> = _lastCompletedLevel.asStateFlow()

    private val _accuracyPercent = MutableStateFlow(100)
    val accuracyPercent: StateFlow<Int> = _accuracyPercent.asStateFlow()

    /** Time taken to clear the last completed level, not counting time spent paused. */
    private val _lastClearTimeMs = MutableStateFlow(0L)
    val lastClearTimeMs: StateFlow<Long> = _lastClearTimeMs.asStateFlow()

    /** Wrong arrows tapped in the last completed level (each one cost a life). */
    private val _lastMistakes = MutableStateFlow(0)
    val lastMistakes: StateFlow<Int> = _lastMistakes.asStateFlow()

    // Level timer
    private var levelStartMs = 0L
    private var pausedTotalMs = 0L
    private var pauseStartedMs: Long? = null

    private fun playTimeMs(): Long {
        val now = SystemClock.elapsedRealtime()
        val currentPause = pauseStartedMs?.let { now - it } ?: 0L
        return (now - levelStartMs - pausedTotalMs - currentPause).coerceAtLeast(0L)
    }

    init {
        startSplashFlow()
    }

    private fun startSplashFlow() {
        viewModelScope.launch {
            // Only splash screen: White background with "Yusr Game Studios"
            _screenState.value = ScreenState.SPLASH
            delay(1500)

            // Load saved game progress or start at Level 1 (generated off the main thread)
            val savedProgress = dao.getProgress() ?: GameProgressEntity()
            val def = withContext(Dispatchers.Default) { LevelRepository.getLevel(savedProgress.currentLevel) }
            startLevel(def)

            // Directly proceed to gameplay!
            _screenState.value = ScreenState.GAMEPLAY
        }
    }

    fun loadLevel(levelNum: Int) {
        startLevel(LevelRepository.getLevel(levelNum))
    }

    private fun startLevel(def: LevelDefinition) {
        val arrowStates = def.arrows.associate { arrow ->
            arrow.id to ActiveArrowState(
                arrow = arrow,
                isRemoved = false,
                isExiting = false,
                exitProgress = 0f,
                isBlocked = false,
                isHighlighted = false
            )
        }.toMutableMap()

        // Compute initial blocked states
        recalculateBlockedStates(def, arrowStates)

        _currentLevelState.value = LevelPlayState(
            definition = def,
            sessionId = SystemClock.elapsedRealtimeNanos(),
            arrows = arrowStates,
            heartsRemaining = 3,
            movesMade = 0,
            errorsMade = 0,
            crackStage = 0,
            isShattering = false
        )
        _shards.value = emptyList()

        levelStartMs = SystemClock.elapsedRealtime()
        pausedTotalMs = 0L
        pauseStartedMs = null

        // Generate the next level in the background so "Next Level" opens instantly
        if (!def.isDaily) {
            viewModelScope.launch(Dispatchers.Default) {
                LevelRepository.getLevel(def.levelNumber + 1)
            }
        }
    }

    private fun recalculateBlockedStates(
        def: LevelDefinition,
        arrowMap: MutableMap<String, ActiveArrowState>
    ) {
        // Arrows that are already sliding out no longer block anything
        val occupied = HashMap<GridPoint, String>()
        arrowMap.values
            .filter { !it.isRemoved && !it.isExiting }
            .forEach { s -> LevelGenerator.cellsOf(s.arrow).forEach { occupied[it] = s.arrow.id } }

        arrowMap.values.filter { !it.isRemoved && !it.isExiting }.forEach { state ->
            val arrow = state.arrow
            arrowMap[arrow.id] = state.copy(isBlocked = isPathObstructed(def, arrow, occupied))
        }
    }

    /** Blocked if and only if another arrow sits anywhere between this arrow's head and the board edge. */
    private fun isPathObstructed(
        def: LevelDefinition,
        arrow: ArrowModel,
        occupied: Map<GridPoint, String>
    ): Boolean {
        var currCol = arrow.head.col + arrow.direction.dx
        var currRow = arrow.head.row + arrow.direction.dy

        while (currCol in 0 until def.cols && currRow in 0 until def.rows) {
            val who = occupied[GridPoint(currCol, currRow)]
            if (who != null && who != arrow.id) return true
            currCol += arrow.direction.dx
            currRow += arrow.direction.dy
        }
        return false
    }

    fun onArrowTapped(arrowId: String) {
        val state = _currentLevelState.value ?: return
        if (state.isShattering || state.isOutOfLives || state.isPaused) return
        val arrowState = state.arrows[arrowId] ?: return
        if (arrowState.isRemoved || arrowState.isExiting) return

        // If chisel tool is active, player can break any blockage directly!
        if (state.isChiselActive) {
            useChiselOnArrow(arrowId)
            return
        }

        if (arrowState.isBlocked) {
            // Blocked arrow: it bumps into its blocker and turns red.
            // Only the first wrong tap on each arrow costs a life; tapping the same red arrow again is free.
            soundManager.playBlockedThud()
            hapticManager.blocked()
            val costsLife = !arrowState.isWrong && state.heartsRemaining > 0
            val newArrows = state.arrows.toMutableMap()
            newArrows[arrowId] = arrowState.copy(
                shakeTrigger = arrowState.shakeTrigger + 1,
                isWrong = true
            )
            val newHearts = if (costsLife) state.heartsRemaining - 1 else state.heartsRemaining
            _currentLevelState.value = state.copy(
                arrows = newArrows,
                errorsMade = state.errorsMade + if (costsLife) 1 else 0,
                heartsRemaining = newHearts,
                isOutOfLives = newHearts == 0
            )
            return
        }

        // Free arrow tapped: it slides out, and anything it was blocking is free straight away
        soundManager.playArrowTap()
        hapticManager.tap()

        val updatedMap = state.arrows.toMutableMap()
        updatedMap[arrowId] = arrowState.copy(isExiting = true, isHighlighted = false)
        recalculateBlockedStates(state.definition, updatedMap)
        _currentLevelState.value = state.copy(
            arrows = updatedMap,
            movesMade = state.movesMade + 1
        )
    }

    fun onArrowExitCompleted(arrowId: String) {
        val currentState = _currentLevelState.value ?: return
        val arrowState = currentState.arrows[arrowId] ?: return
        if (arrowState.isRemoved || !arrowState.isExiting) return

        val newArrows = currentState.arrows.toMutableMap()
        newArrows[arrowId] = arrowState.copy(isRemoved = true, isExiting = false, exitProgress = 1f)

        // Update crack stage with every arrow going out, and crack open completely on the last arrow!
        val removedCount = newArrows.values.count { it.isRemoved }
        val total = currentState.definition.arrows.size
        val isLastArrow = removedCount == total
        val newCrackStage = when {
            isLastArrow -> 5 // Crack open completely!
            removedCount == 0 -> 0
            total <= 4 -> removedCount
            else -> ((removedCount.toFloat() / (total - 1)) * 4).toInt().coerceIn(1, 4)
        }

        // Sound and haptic feedback for crack
        soundManager.playIceCrack()
        hapticManager.crack()

        // Recalculate remaining arrows blocked state purely based on geometric paths
        recalculateBlockedStates(currentState.definition, newArrows)

        val updatedHistory = currentState.moveHistory + arrowId

        _currentLevelState.value = currentState.copy(
            arrows = newArrows,
            crackStage = newCrackStage,
            moveHistory = updatedHistory
        )

        // Check if level is cleared!
        if (newArrows.values.all { it.isRemoved }) {
            _lastClearTimeMs.value = playTimeMs()
            _lastMistakes.value = currentState.errorsMade
            triggerLevelShatterSequence(currentState.definition, currentState.errorsMade)
        }
    }

    private fun triggerLevelShatterSequence(def: LevelDefinition, errors: Int) {
        viewModelScope.launch {
            delay(200)
            soundManager.playIceShatter()
            hapticManager.shatter()

            // Generate crystal shards for physical shatter animation
            _shards.value = generateShards()

            _currentLevelState.value = _currentLevelState.value?.copy(
                isShattering = true,
                crackStage = 5
            )

            delay(1500)
            // The player may have left or restarted during the shatter animation
            val stillHere = _currentLevelState.value?.let { it.definition == def && it.isShattering } == true
            if (!stillHere || _screenState.value != ScreenState.GAMEPLAY) return@launch
            soundManager.playVictoryChord()
            hapticManager.celebrate()

            // Save progress
            val accuracy = if (errors == 0) 100 else (100 - (errors * 15)).coerceIn(60, 95)
            _accuracyPercent.value = accuracy
            _lastCompletedLevel.value = def

            saveLevelCompletion(def, accuracy)

            // Open Level Complete screen
            _screenState.value = ScreenState.LEVEL_COMPLETE
        }
    }

    private suspend fun saveLevelCompletion(def: LevelDefinition, accuracy: Int) {
        val currentProgress = dao.getProgress() ?: GameProgressEntity()
        val isPerfect = accuracy == 100

        // Day streak: +1 for the first clear on a new consecutive day, back to 1 after a missed day
        val today = localEpochDay()
        val lastPlayed = currentProgress.lastPlayedDate.toLongOrNull()
        val newStreak = when (lastPlayed) {
            today -> currentProgress.streak.coerceAtLeast(1)
            today - 1 -> currentProgress.streak + 1
            else -> 1
        }

        // Replaying an old level or the daily challenge never moves progress backwards or skips ahead
        val nextLvl = def.levelNumber + 1
        val updatedProgress = currentProgress.copy(
            currentLevel = if (def.isDaily) currentProgress.currentLevel else maxOf(currentProgress.currentLevel, nextLvl),
            highestUnlockedLevel = if (def.isDaily) currentProgress.highestUnlockedLevel
            else maxOf(currentProgress.highestUnlockedLevel, nextLvl),
            crystals = currentProgress.crystals + def.rewardCrystals,
            coins = currentProgress.coins + def.rewardCoins,
            perfectLevelsCount = currentProgress.perfectLevelsCount + (if (isPerfect) 1 else 0),
            streak = newStreak,
            lastPlayedDate = today.toString() // stored as the local epoch day
        )
        dao.insertOrUpdateProgress(updatedProgress)

        if (!def.isDaily) {
            val stars = if (accuracy >= 95) 3 else if (accuracy >= 80) 2 else 1
            val previous = dao.getLevelRecord(def.levelNumber)
            // Keep the best result when a level is replayed
            dao.recordLevelCompletion(
                LevelRecordEntity(
                    levelNumber = def.levelNumber,
                    stars = maxOf(stars, previous?.stars ?: 0),
                    bestAccuracy = maxOf(accuracy, previous?.bestAccuracy ?: 0),
                    isCompleted = true
                )
            )
        }

        updateAchievementsProgress(updatedProgress)
    }

    /** Recomputes every achievement from the saved progress, keeping whether its reward was claimed. */
    private suspend fun updateAchievementsProgress(progress: GameProgressEntity) {
        dao.insertInitialAchievements(AppDatabase.defaultAchievements) // no-op if they already exist
        val levelsCleared = (progress.highestUnlockedLevel - 1).coerceAtLeast(0)
        val existing = dao.getAllAchievementsFlow().first().associateBy { it.id }

        AppDatabase.defaultAchievements.forEach { default ->
            val value = when (default.id) {
                "ach_perfect_break" -> progress.perfectLevelsCount
                "ach_crystal_hunter" -> progress.crystals
                else -> levelsCleared // ice breaker, glacial apprentice, frozen master, shatter king
            }
            val current = existing[default.id] ?: default
            dao.updateAchievement(
                current.copy(
                    currentProgress = value.coerceAtMost(current.targetProgress),
                    isUnlocked = value >= current.targetProgress
                )
            )
        }
    }

    private fun generateShards(): List<CrystalShard> {
        val list = mutableListOf<CrystalShard>()
        val colors = listOf(0xFFBCE7FF, 0xFF9BE7FF, 0xFFFFFFFF, 0xFF65B9F3, 0xFFE0F4FF)
        for (i in 0 until 28) {
            val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
            val speed = 250f + Random.nextFloat() * 450f
            list.add(
                CrystalShard(
                    id = i,
                    startX = 0.5f + (Random.nextFloat() - 0.5f) * 0.4f,
                    startY = 0.5f + (Random.nextFloat() - 0.5f) * 0.4f,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    rotation = Random.nextFloat() * 360f,
                    vRotation = (Random.nextFloat() - 0.5f) * 400f,
                    size = 14f + Random.nextFloat() * 22f,
                    colorHex = colors[i % colors.size]
                )
            )
        }
        return list
    }

    fun onNextLevelTapped() {
        val def = _currentLevelState.value?.definition
        if (def == null || def.isDaily) {
            // After the daily challenge, "next" goes back to the player's own level
            onContinueGameTapped()
            return
        }
        loadLevel(def.levelNumber + 1)
        _screenState.value = ScreenState.GAMEPLAY
    }

    /** Restarts whatever was just played, including the daily challenge. */
    fun onReplayLevelTapped() {
        val def = _currentLevelState.value?.definition ?: LevelRepository.getLevel(1)
        startLevel(def)
        _screenState.value = ScreenState.GAMEPLAY
    }

    /** Resumes the level in progress if it's the player's current one, otherwise loads it fresh. */
    fun onContinueGameTapped() {
        viewModelScope.launch {
            val level = (dao.getProgress() ?: GameProgressEntity()).currentLevel
            val state = _currentLevelState.value
            val canResume = state != null && !state.definition.isDaily &&
                state.definition.levelNumber == level && !state.isAllCleared && !state.isShattering
            if (!canResume) loadLevel(level) else if (!state!!.isPaused) resumeTimer()
            _screenState.value = ScreenState.GAMEPLAY
        }
    }

    /** Always starts the player's current level from the beginning. */
    fun onRestartGameTapped() {
        viewModelScope.launch {
            loadLevel((dao.getProgress() ?: GameProgressEntity()).currentLevel)
            _screenState.value = ScreenState.GAMEPLAY
        }
    }

    fun onPlayDailyChallengeTapped() {
        startLevel(LevelRepository.getDailyLevel(localEpochDay()))
        _screenState.value = ScreenState.GAMEPLAY
    }

    fun onMainMenuTapped() {
        // Leaving a level in progress pauses its clear timer
        if (_screenState.value == ScreenState.GAMEPLAY) pauseTimer()
        _screenState.value = ScreenState.MAIN_MENU
    }

    fun onDailyChallengeTapped() {
        _screenState.value = ScreenState.DAILY_CHALLENGE
    }

    fun onProfileTapped() {
        _screenState.value = ScreenState.PROFILE
    }

    // Boosters / Tools
    fun onHintTapped() {
        val state = _currentLevelState.value ?: return
        val availableArrow = state.arrows.values.firstOrNull { !it.isRemoved && !it.isExiting && !it.isBlocked } ?: return

        viewModelScope.launch {
            val progress = dao.getProgress() ?: GameProgressEntity()
            if (progress.hintCount > 0) {
                dao.insertOrUpdateProgress(progress.copy(hintCount = progress.hintCount - 1))
            } else {
                // Watched ad for hint!
                soundManager.playVictoryChord()
            }
        }

        soundManager.playArrowTap()
        hapticManager.tap()

        val updatedMap = state.arrows.toMutableMap()
        updatedMap[availableArrow.arrow.id] = availableArrow.copy(isHighlighted = true)
        _currentLevelState.value = state.copy(arrows = updatedMap)

        // Clear highlight after 2.5 seconds
        viewModelScope.launch {
            delay(2500)
            val s = _currentLevelState.value ?: return@launch
            val cur = s.arrows[availableArrow.arrow.id] ?: return@launch
            val m = s.arrows.toMutableMap()
            m[availableArrow.arrow.id] = cur.copy(isHighlighted = false)
            _currentLevelState.value = s.copy(arrows = m)
        }
    }

    fun onChiselTapped() {
        val state = _currentLevelState.value ?: return
        val newChisel = !state.isChiselActive
        _currentLevelState.value = state.copy(
            isChiselActive = newChisel,
            isFreezeActive = false
        )
        soundManager.playArrowTap()
        hapticManager.tap()
    }

    private fun useChiselOnArrow(arrowId: String) {
        val state = _currentLevelState.value ?: return
        val arrowState = state.arrows[arrowId] ?: return
        soundManager.playChiselHit()
        hapticManager.crack()

        val updatedMap = state.arrows.toMutableMap()
        // Unblock this arrow directly with the chisel!
        updatedMap[arrowId] = arrowState.copy(isBlocked = false, isHighlighted = true)
        _currentLevelState.value = state.copy(
            arrows = updatedMap,
            isChiselActive = false
        )
    }

    fun onUndoTapped() {
        val state = _currentLevelState.value ?: return
        if (state.moveHistory.isEmpty()) return
        val lastId = state.moveHistory.last()
        val arrowState = state.arrows[lastId] ?: return

        soundManager.playArrowTap()
        hapticManager.tap()

        val updatedMap = state.arrows.toMutableMap()
        updatedMap[lastId] = arrowState.copy(isRemoved = false, isExiting = false, exitProgress = 0f)

        val newHistory = state.moveHistory.dropLast(1)
        val removedCount = updatedMap.values.count { it.isRemoved }
        val total = state.definition.arrows.size
        val ratio = removedCount.toFloat() / total.coerceAtLeast(1)
        val newCrackStage = when {
            ratio >= 1.0f -> 5
            ratio >= 0.8f -> 4
            ratio >= 0.6f -> 3
            ratio >= 0.4f -> 2
            ratio >= 0.2f -> 1
            else -> 0
        }

        recalculateBlockedStates(state.definition, updatedMap)

        _currentLevelState.value = state.copy(
            arrows = updatedMap,
            moveHistory = newHistory,
            crackStage = newCrackStage
        )
    }

    fun onFreezeTapped() {
        val state = _currentLevelState.value ?: return
        val newFreeze = !state.isFreezeActive
        _currentLevelState.value = state.copy(
            isFreezeActive = newFreeze,
            isChiselActive = false
        )
        soundManager.playArrowTap()
        hapticManager.tap()
    }

    fun togglePause() {
        val state = _currentLevelState.value ?: return
        val pausing = !state.isPaused
        if (pausing) pauseTimer() else resumeTimer()
        _currentLevelState.value = state.copy(isPaused = pausing)
    }

    /** Days since 1970-01-01 in the phone's time zone (java.time needs API 26, the app supports 24+). */
    private fun localEpochDay(): Long {
        val now = System.currentTimeMillis()
        return (now + TimeZone.getDefault().getOffset(now)) / 86_400_000L
    }

    private fun pauseTimer() {
        if (pauseStartedMs == null) pauseStartedMs = SystemClock.elapsedRealtime()
    }

    private fun resumeTimer() {
        pauseStartedMs?.let { pausedTotalMs += SystemClock.elapsedRealtime() - it }
        pauseStartedMs = null
    }

    fun toggleRestartDialog(show: Boolean) {
        val state = _currentLevelState.value ?: return
        _currentLevelState.value = state.copy(showRestartDialog = show)
    }

    fun toggleSettingsDialog(show: Boolean) {
        val state = _currentLevelState.value ?: return
        _currentLevelState.value = state.copy(showSettingsDialog = show)
    }

    fun refillLivesAndContinue() {
        val state = _currentLevelState.value ?: return
        _currentLevelState.value = state.copy(
            heartsRemaining = 3,
            isOutOfLives = false
        )
        soundManager.playVictoryChord()
        hapticManager.celebrate()
    }

    fun toggleGrid() {
        val state = _currentLevelState.value ?: return
        _currentLevelState.value = state.copy(
            isGridActive = !state.isGridActive
        )
        soundManager.playArrowTap()
        hapticManager.tap()
    }

    override fun onCleared() {
        soundManager.release()
        super.onCleared()
    }

    fun updateSettings(sound: Boolean, music: Boolean, haptics: Boolean) {
        viewModelScope.launch {
            val curr = dao.getProgress() ?: GameProgressEntity()
            dao.insertOrUpdateProgress(
                curr.copy(
                    soundEnabled = sound,
                    musicEnabled = music,
                    hapticsEnabled = haptics
                )
            )
        }
    }
}
