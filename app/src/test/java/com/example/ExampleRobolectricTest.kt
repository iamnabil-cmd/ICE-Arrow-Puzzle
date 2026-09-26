package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.LevelRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ICE ARROW PUZZLE", appName)
    }

    @Test
    fun `verify level repository has 150 playable levels`() {
        val level1 = LevelRepository.getLevel(1)
        assertNotNull(level1)
        assertEquals("Level 1", level1.title)
        assertTrue(level1.arrows.size >= 3)
        assertTrue("Level 1 must be 100% solvable", LevelRepository.canSolveLevel(level1.arrows, level1.cols, level1.rows))

        // Level 2 verification (ensuring it is playable, not all blocked like in Video 1)
        val level2 = LevelRepository.getLevel(2)
        assertNotNull(level2)
        assertEquals("Level 2", level2.title)
        assertTrue("Level 2 must be 100% solvable without deadlocks", LevelRepository.canSolveLevel(level2.arrows, level2.cols, level2.rows))

        // Level 28 verification (no random curves, all square rectilinear turns, 100% solvable)
        val level28 = LevelRepository.getLevel(28)
        assertNotNull(level28)
        assertTrue("Level 28 must be 100% solvable", LevelRepository.canSolveLevel(level28.arrows, level28.cols, level28.rows))
        for (arrow in level28.arrows) {
            for (i in 0 until arrow.points.size - 1) {
                val p1 = arrow.points[i]
                val p2 = arrow.points[i + 1]
                assertTrue("Segments must be strictly horizontal or vertical (no random curves)", p1.col == p2.col || p1.row == p2.row)
            }
        }

        val level20 = LevelRepository.getLevel(20)
        assertNotNull(level20)
        assertTrue("Level 20 should have at least 18 arrows", level20.arrows.size >= 18)
        assertTrue("Level 20 must be 100% solvable", LevelRepository.canSolveLevel(level20.arrows, level20.cols, level20.rows))

        val level50 = LevelRepository.getLevel(50)
        assertNotNull(level50)
        assertTrue("Level 50 should have at least 25 arrows", level50.arrows.size >= 25)
        assertTrue("Level 50 must be 100% solvable", LevelRepository.canSolveLevel(level50.arrows, level50.cols, level50.rows))

        val level150 = LevelRepository.getLevel(150)
        assertNotNull(level150)
        assertTrue(level150.arrows.isNotEmpty())
        assertTrue("Level 150 must be 100% solvable", LevelRepository.canSolveLevel(level150.arrows, level150.cols, level150.rows))
    }
}
