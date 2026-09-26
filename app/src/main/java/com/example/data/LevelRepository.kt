package com.example.data

/**
 * Supplies levels. Level 1 is a hand-made tutorial; every level after it comes from
 * [LevelGenerator], so there is no upper limit on the number of levels.
 */
object LevelRepository {

    private val cache = HashMap<Int, LevelDefinition>()

    fun getLevel(levelNumber: Int): LevelDefinition {
        val n = levelNumber.coerceAtLeast(1)
        return synchronized(cache) {
            cache.getOrPut(n) { if (n == 1) createLevel1() else generateLevel(n) }
        }
    }

    /**
     * Level 1 (tutorial, like the first level in the reference video):
     * 3 straight arrows, none blocked.
     */
    private fun createLevel1(): LevelDefinition {
        val arrows = listOf(
            ArrowModel("l1_1", listOf(GridPoint(1, 3), GridPoint(1, 1)), ArrowDirection.UP),
            ArrowModel("l1_2", listOf(GridPoint(2, 3), GridPoint(2, 1)), ArrowDirection.UP),
            ArrowModel("l1_3", listOf(GridPoint(3, 1), GridPoint(3, 3)), ArrowDirection.DOWN)
        )

        return LevelDefinition(
            levelNumber = 1,
            title = "Level 1",
            subtitle = "SILVER FROST SANCTUARY",
            cols = 5,
            rows = 5,
            arrows = arrows,
            movesAllowed = 5,
            rewardCrystals = 100,
            rewardCoins = 200,
            rewardItemName = "Silver Frost Coin",
            rewardDrawableRes = com.example.R.drawable.img_reward_silver_coin,
            tutorialHint = "Tap unblocked arrows to slide them out!"
        )
    }

    private fun generateLevel(levelNum: Int): LevelDefinition {
        val difficulty = LevelGenerator.difficultyFor(levelNum)
        val arrows = LevelGenerator.generate(levelNum, difficulty)
        val (rewardName, rewardRes) = rewardFor(levelNum)

        return LevelDefinition(
            levelNumber = levelNum,
            title = "Level $levelNum",
            subtitle = subtitleFor(levelNum),
            cols = difficulty.cols,
            rows = difficulty.rows,
            arrows = arrows,
            movesAllowed = arrows.size + 5,
            rewardCrystals = 100 + (levelNum * 12),
            rewardCoins = 200 + (levelNum * 18),
            rewardItemName = rewardName,
            rewardDrawableRes = rewardRes
        )
    }

    private fun subtitleFor(levelNum: Int) = when {
        levelNum <= 15 -> "SILVER FROST SANCTUARY"
        levelNum <= 35 -> "GOLDEN BOREALIS VAULT"
        levelNum <= 60 -> "DIAMOND PERMAFROST"
        levelNum <= 100 -> "GLACIAL CITADEL"
        else -> "APEX ICE PINNACLE"
    }

    /** Milestone rewards every 25 levels, and a rotating collectible in between. */
    private fun rewardFor(levelNum: Int): Pair<String, Int> {
        if (levelNum % 25 == 0) {
            return when ((levelNum / 25) % 3) {
                1 -> "Glacial Chisel" to com.example.R.drawable.img_reward_chisel
                2 -> "Mythic Star Warhammer" to com.example.R.drawable.img_reward_hammer
                else -> "Solar Heat Gun" to com.example.R.drawable.img_reward_heatgun
            }
        }
        return when {
            levelNum <= 10 -> "Silver Frost Coin" to com.example.R.drawable.img_reward_silver_coin
            levelNum <= 15 -> "Glacial Silver Sovereign" to com.example.R.drawable.img_reward_silver_coin
            levelNum <= 35 -> "Gold Sovereign Coin" to com.example.R.drawable.img_reward_coin
            levelNum <= 60 -> "Brilliant Ice Diamond" to com.example.R.drawable.img_reward_diamond
            levelNum <= 85 -> "Ice Crystal Trophy" to com.example.R.drawable.img_reward_trophy
            else -> when (levelNum % 4) {
                0 -> "Royal Sovereign Gold" to com.example.R.drawable.img_reward_coin
                1 -> "Crown Jewel Diamond" to com.example.R.drawable.img_reward_diamond
                2 -> "Apex Crystal Trophy" to com.example.R.drawable.img_reward_trophy
                else -> "Glacial Silver Sovereign" to com.example.R.drawable.img_reward_silver_coin
            }
        }
    }
}
