package com.example.game

import com.example.data.ArrowDirection
import com.example.data.ArrowModel
import com.example.data.GridPoint
import com.example.data.LevelDefinition

enum class ScreenState {
    SPLASH,
    GAMEPLAY,
    LEVEL_COMPLETE,
    MAIN_MENU,
    DAILY_CHALLENGE,
    PROFILE
}

enum class BoosterType {
    NONE,
    HINT,
    CHISEL,
    UNDO,
    FREEZE
}

data class ActiveArrowState(
    val arrow: ArrowModel,
    val isRemoved: Boolean = false,
    val isExiting: Boolean = false,
    val exitProgress: Float = 0f, // 0f to 1f
    val isBlocked: Boolean = false,
    val isHighlighted: Boolean = false,
    val shakeTrigger: Int = 0,
    // Set the first time this arrow is tapped while blocked; that tap costs a life, repeat taps don't
    val isWrong: Boolean = false
)

data class CrystalShard(
    val id: Int,
    val startX: Float,
    val startY: Float,
    val vx: Float,
    val vy: Float,
    val rotation: Float,
    val vRotation: Float,
    val size: Float,
    val colorHex: Long
)

data class LevelPlayState(
    val definition: LevelDefinition,
    val arrows: Map<String, ActiveArrowState> = emptyMap(),
    val heartsRemaining: Int = 3,
    val movesMade: Int = 0,
    val errorsMade: Int = 0,
    val crackStage: Int = 0, // 0 to 5
    val isShattering: Boolean = false,
    val activeBooster: BoosterType = BoosterType.NONE,
    val moveHistory: List<String> = emptyList(), // Arrow IDs removed in order
    val isChiselActive: Boolean = false,
    val isFreezeActive: Boolean = false,
    val isPaused: Boolean = false,
    val isOutOfLives: Boolean = false,
    val isGridActive: Boolean = false,
    val showRestartDialog: Boolean = false,
    val showSettingsDialog: Boolean = false
) {
    val totalRequiredArrows: Int get() = definition.arrows.size
    val removedArrowsCount: Int get() = arrows.values.count { it.isRemoved }
    val remainingArrowsCount: Int get() = totalRequiredArrows - removedArrowsCount
    val isAllCleared: Boolean get() = arrows.isNotEmpty() && arrows.values.all { it.isRemoved }
}
