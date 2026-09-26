package com.example.data

enum class ArrowDirection {
    UP, DOWN, LEFT, RIGHT;

    val dx: Int
        get() = when (this) {
            LEFT -> -1
            RIGHT -> 1
            else -> 0
        }

    val dy: Int
        get() = when (this) {
            UP -> -1
            DOWN -> 1
            else -> 0
        }

    val angleDegrees: Float
        get() = when (this) {
            UP -> 0f
            RIGHT -> 90f
            DOWN -> 180f
            LEFT -> 270f
        }
}

data class GridPoint(
    val col: Int,
    val row: Int
)

data class ArrowModel(
    val id: String,
    // The path points in grid coordinates, from tail to head
    val points: List<GridPoint>,
    val direction: ArrowDirection,
    val isHidden: Boolean = false,
    val isChiseled: Boolean = false,
    // If empty, availability is determined by ray-tracing to edge of grid
    val explicitBlockedBy: List<String> = emptyList()
) {
    val head: GridPoint get() = points.last()
    val tail: GridPoint get() = points.first()
}

data class LevelDefinition(
    val levelNumber: Int,
    val title: String,
    val subtitle: String = "GLACIAL SANCTUARY",
    val cols: Int,
    val rows: Int,
    val arrows: List<ArrowModel>,
    val movesAllowed: Int = arrows.size + 3,
    val rewardCrystals: Int = 150,
    val rewardCoins: Int = 250,
    val hiddenRewardType: String? = null, // e.g. "CRYSTAL_TROPHY", "GOLD_STAR", "ICE_RELIC"
    val rewardItemName: String = "Star Hammer",
    val rewardDrawableRes: Int? = null,
    val tutorialHint: String? = null, // e.g. "Tap unblocked arrows to slide them out of the ice"
    // Daily challenge levels don't advance the player's level progress
    val isDaily: Boolean = false
)
