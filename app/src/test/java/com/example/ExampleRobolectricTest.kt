package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.LevelGenerator
import com.example.data.LevelRepository
import org.junit.Assert.assertEquals
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
    fun `levels load with rewards and are solvable`() {
        val level1 = LevelRepository.getLevel(1)
        assertEquals("Level 1", level1.title)
        assertEquals(3, level1.arrows.size)

        for (n in listOf(1, 2, 20, 50, 150, 500)) {
            val def = LevelRepository.getLevel(n)
            assertEquals("Level $n", def.title)
            assertTrue("Level $n needs a reward image", def.rewardDrawableRes != null)
            val result = LevelGenerator.validate(def.arrows, def.cols, def.rows)
            assertTrue("Level $n: ${result.reason}", result.isValid)
        }
    }
}
