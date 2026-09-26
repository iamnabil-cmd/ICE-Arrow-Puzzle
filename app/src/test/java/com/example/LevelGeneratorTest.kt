package com.example

import com.example.data.GridPoint
import com.example.data.LevelGenerator
import com.example.data.LevelRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelGeneratorTest {

    private val sampleLevels = (1..300) + listOf(500, 1_000, 2_500, 10_000)

    @Test
    fun everyLevelIsValidAndSolvable() {
        for (n in sampleLevels) {
            val def = LevelRepository.getLevel(n)
            val result = LevelGenerator.validate(def.arrows, def.cols, def.rows)
            assertTrue("Level $n invalid: ${result.reason}", result.isValid)
        }
    }

    @Test
    fun arrowsNeverShareADot() {
        for (n in sampleLevels) {
            val def = LevelRepository.getLevel(n)
            val seen = HashSet<GridPoint>()
            def.arrows.forEach { arrow ->
                LevelGenerator.cellsOf(arrow).forEach { cell ->
                    assertTrue("Level $n: ${arrow.id} overlaps at $cell", seen.add(cell))
                }
            }
        }
    }

    @Test
    fun levelsAreDeterministic() {
        val a = LevelGenerator.generate(42)
        val b = LevelGenerator.generate(42)
        assertEquals(a, b)
    }

    @Test
    fun difficultyRampsUp() {
        fun avgArrows(range: IntRange) = range.map { LevelRepository.getLevel(it).arrows.size }.average()
        fun avgDepth(range: IntRange) = range.map {
            val def = LevelRepository.getLevel(it)
            LevelGenerator.validate(def.arrows, def.cols, def.rows).depth
        }.average()

        assertTrue(avgArrows(2..10) < avgArrows(20..30))
        assertTrue(avgArrows(20..30) < avgArrows(60..80))
        assertTrue(avgDepth(2..10) < avgDepth(60..80))
    }

    @Test
    fun validatorRejectsOverlapsAndDeadlocks() {
        val overlapping = listOf(
            com.example.data.ArrowModel("a", listOf(GridPoint(0, 1), GridPoint(3, 1)), com.example.data.ArrowDirection.RIGHT),
            com.example.data.ArrowModel("b", listOf(GridPoint(2, 3), GridPoint(2, 0)), com.example.data.ArrowDirection.UP)
        )
        assertTrue(!LevelGenerator.validate(overlapping, 5, 5).isValid)

        // Two arrows pointing at each other can never leave
        val deadlock = listOf(
            com.example.data.ArrowModel("a", listOf(GridPoint(0, 1), GridPoint(1, 1)), com.example.data.ArrowDirection.RIGHT),
            com.example.data.ArrowModel("b", listOf(GridPoint(4, 1), GridPoint(3, 1)), com.example.data.ArrowDirection.LEFT)
        )
        assertTrue(!LevelGenerator.validate(deadlock, 5, 5).isValid)
    }
}
