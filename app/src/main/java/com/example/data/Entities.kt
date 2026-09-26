package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_progress")
data class GameProgressEntity(
    @PrimaryKey val id: Int = 1,
    val currentLevel: Int = 1,
    val highestUnlockedLevel: Int = 1,
    val coins: Int = 500,
    val crystals: Int = 200,
    val hintCount: Int = 2,
    val chiselCount: Int = 2,
    val undoCount: Int = 5,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val streak: Int = 1,
    val lastPlayedDate: String = "",
    val perfectLevelsCount: Int = 0,
    val totalToolsUsed: Int = 0
)

@Entity(tableName = "level_records")
data class LevelRecordEntity(
    @PrimaryKey val levelNumber: Int,
    val stars: Int = 3,
    val bestAccuracy: Int = 100,
    val isCompleted: Boolean = true,
    val completedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val currentProgress: Int = 0,
    val targetProgress: Int = 1,
    val isUnlocked: Boolean = false,
    val isClaimed: Boolean = false,
    val rewardCrystals: Int = 50
)
