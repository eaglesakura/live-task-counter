package com.eaglesakura.firearm.experimental.coroutines

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eaglesakura.armyknife.android.junit4.extensions.compatibleBlockingTest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SingleTaskTest {

    @Test
    fun cancel() = compatibleBlockingTest {
        val task = SingleTask()
        var started = false
        launch {
            task.run {
                started = true
                delay(1000)
                yield()
                assertFalse(isActive)
                fail("DON'T RUN THIS, isActive='$isActive'")
            }
        }

        while (!started) {
            yield()
        }
        task.cancelAndJoin()
        assertNull(task.scope)
    }

    @Test
    fun run_foreground() = compatibleBlockingTest(Dispatchers.Main) {
        val task = SingleTask()
        assertFalse(task.running.value!!)
        assertTrue(task.run {
            true
        })
    }

    @Test
    fun run_background() = compatibleBlockingTest {
        val task = SingleTask()
        assertFalse(task.running.value!!)
        assertTrue(task.run {
            true
        })
    }

    @Test
    fun run_parallel() = compatibleBlockingTest(Dispatchers.Main) {
        val task = SingleTask()

        val first = async {
            task.run("first") {
                delay(10000)
                assertFalse(isActive)
                fail("DON'T RUN THIS, isActive='$isActive'")
                "first"
            }
        }

        val second = async {
            task.run("second") {
                delay(100)
                "second"
            }
        }

        delay(10)
        assertTrue(task.running.value!!)
        assertEquals(task.isRunning, task.running.value)

        try {
            first.await()
            fail()
        } catch (e: CancellationException) {
        }

        assertTrue(task.running.value!!)
        assertEquals(task.isRunning, task.running.value)
        assertEquals("second", second.await())
        assertFalse(task.running.value!!)
        assertEquals(task.isRunning, task.running.value)
    }

    @Test
    fun join() = compatibleBlockingTest {
        val task = withContext(Dispatchers.Main) { SingleTask() }

        assertEquals(task.isRunning, task.running.value)

        assertTrue(task.run { true })
        task.join()
        assertTrue(task.run { true })
        task.join()
        assertFalse(task.running.value!!)
        assertEquals(task.isRunning, task.running.value)
    }
}