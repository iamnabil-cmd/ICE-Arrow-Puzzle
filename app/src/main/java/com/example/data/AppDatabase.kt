package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        GameProgressEntity::class,
        LevelRecordEntity::class,
        AchievementEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ice_arrow_puzzle.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getInstance(context).gameDao()
                            dao.insertOrUpdateProgress(GameProgressEntity())
                            dao.insertInitialAchievements(defaultAchievements)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        val defaultAchievements = listOf(
            AchievementEntity(
                id = "ach_ice_breaker",
                title = "ICE BREAKER",
                description = "Complete your first ice puzzle level.",
                targetProgress = 1,
                rewardCrystals = 50
            ),
            AchievementEntity(
                id = "ach_permafrost",
                title = "GLACIAL APPRENTICE",
                description = "Clear 5 challenging levels.",
                targetProgress = 5,
                rewardCrystals = 100
            ),
            AchievementEntity(
                id = "ach_frozen_master",
                title = "FROZEN MASTER",
                description = "Conquer 20 intricate ice puzzles.",
                targetProgress = 20,
                rewardCrystals = 250
            ),
            AchievementEntity(
                id = "ach_perfect_break",
                title = "PERFECT BREAK",
                description = "Solve 3 levels with 100% accuracy without tools.",
                targetProgress = 3,
                rewardCrystals = 150
            ),
            AchievementEntity(
                id = "ach_crystal_hunter",
                title = "CRYSTAL HUNTER",
                description = "Accumulate 1,000 shining crystals.",
                targetProgress = 1000,
                rewardCrystals = 300
            ),
            AchievementEntity(
                id = "ach_shatter_king",
                title = "SHATTER KING",
                description = "Completely shatter 15 glacial blocks.",
                targetProgress = 15,
                rewardCrystals = 200
            )
        )
    }
}
