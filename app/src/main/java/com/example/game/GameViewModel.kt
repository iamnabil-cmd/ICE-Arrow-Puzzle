package com.example.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.data.AchievementEntity
import com.example.data.AppDatabase
import com.example.data.ArrowDirection
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.gameDao()

    val progressFlow: StateFlow<GameProgressEntity?> = dao.getProgressFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val achievementsFlow = dao.getAllAchievementsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val soundManager = SoundManager {
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

    init {
        startSplashFlow()
    }

    private fun startSplashFlow() {
        viewModelScope.launch {
            // Only splash screen: White background with "Yusr Game Studios"
            _screenState.value = ScreenState.SPLASH
            delay(1500)

            // Load saved game progress or start at Level 1
            val savedProgress = dao.getProgress() ?: GameProgressEntity()
            loadLevel(savedProgress.currentLevel)

            // Directly proceed to gameplay!
            _screenState.value = ScreenState.GAMEPLAY
        }
    }

    fun loadLevel(levelNum: Int) {
        val def = LevelRepository.getLevel(levelNum)
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
            arrows = arrowStates,
            heartsRemaining = 3,
            movesMade = 0,
            errorsMade = 0,
            crackStage = 0,
            isShattering = false
        )
        _shards.value = emptyList()

        // Generate the next level in the background so "Next Level" opens instantly
        viewModelScope.launch(Dispatchers.Default) {
            LevelRepository.getLevel(levelNum + 1)
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
        soundManager.playArrowWhoosh()
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
        val nextLvl = def.levelNumber + 1
        val newHighest = maxOf(currentProgress.highestUnlockedLevel, nextLvl)
        val isPerfect = accuracy == 100

        val updatedProgress = currentProgress.copy(
            currentLevel = nextLvl,
            highestUnlockedLevel = newHighest,
            crystals = currentProgress.crystals + def.rewardCrystals,
            coins = currentProgress.coins + def.rewardCoins,
            perfectLevelsCount = currentProgress.perfectLevelsCount + (if (isPerfect) 1 else 0),
            streak = currentProgress.streak + 1
        )
        dao.insertOrUpdateProgress(updatedProgress)

        dao.recordLevelCompletion(
            LevelRecordEntity(
                levelNumber = def.levelNumber,
                stars = if (accuracy >= 95) 3 else if (accuracy >= 80) 2 else 1,
                bestAccuracy = accuracy,
                isCompleted = true
            )
        )

        // Check achievements
        updateAchievementsProgress(updatedProgress)
    }

    private suspend fun updateAchievementsProgress(progress: GameProgressEntity) {
        val achievements = dao.getAllAchievementsFlow()
        // Simple helper to mark achievements
        // "ICE BREAKER"
        if (progress.currentLevel >= 2) {
            dao.updateAchievement(
                AchievementEntity(
                    id = "ach_ice_breaker",
                    title = "ICE BREAKER",
                    description = "Complete your first ice puzzle level.",
                    currentProgress = 1,
                    targetProgress = 1,
                    isUnlocked = true,
                    rewardCrystals = 50
                )
            )
        }
        if (progress.currentLevel >= 6) {
            dao.updateAchievement(
                AchievementEntity(
                    id = "ach_permafrost",
                    title = "GLACIAL APPRENTICE",
                    description = "Clear 5 challenging levels.",
                    currentProgress = 5,
                    targetProgress = 5,
                    isUnlocked = true,
                    rewardCrystals = 100
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
        val cur = _currentLevelState.value?.definition?.levelNumber ?: 1
        val nextLevel = cur + 1
        loadLevel(nextLevel)
        _screenState.value = ScreenState.GAMEPLAY
    }

    fun onReplayLevelTapped() {
        val cur = _currentLevelState.value?.definition?.levelNumber ?: 1
        loadLevel(cur)
        _screenState.value = ScreenState.GAMEPLAY
    }

    fun onMainMenuTapped() {
        _screenState.value = ScreenState.MAIN_MENU
    }

    fun onDailyChallengeTapped() {
        _screenState.value = ScreenState.DAILY_CHALLENGE
    }

    fun onProfileTapped() {
        _screenState.value = ScreenState.PROFILE
    }

    fun onBackToGameplay() {
        _screenState.value = ScreenState.GAMEPLAY
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
        _currentLevelState.value = state.copy(isPaused = !state.isPaused)
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
