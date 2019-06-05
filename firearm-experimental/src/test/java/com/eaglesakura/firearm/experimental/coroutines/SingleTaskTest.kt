package com.eaglesakura.firearm.experimental.coroutines

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eaglesakura.armyknife.android.junit4.extensions.compatibleBlockingTest
import com.eaglesakura.armyknife.runtime.coroutines.FlexibleThreadPoolDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SingleTaskTest {

    @Test
    fun run() = compatibleBlockingTest {
        val task = SingleTask()
        assertTrue(task.run {
            true
        })
    }

    @Test
    fun launch_and_launch() = compatibleBlockingTest {
        val task = SingleTask()
        val channel = Channel<Int>()
        task.launch {
            delay(500)
            yield()
            channel.send(1)
            fail("DON'T CALL HERE")
        }
        val job = task.launch {
            channel.send(2)
        }

        assertEquals(2, channel.receive())
        job.join()
        assertFalse(task.isActive)
    }

    @Test
    fun async_and_async() = compatibleBlockingTest {
        val task = SingleTask()
        val job0 = task.async {
            delay(10000)
            yield()
            1
        }
        val job1 = task.async {
            2
        }

        try {
            val result = job0.await()
            fail("Not cancel = '$result'")
        } catch (e: CancellationException) {
            // ok
        }

        assertEquals(2, job1.await())
        assertFalse(task.isActive)
    }

    @Test
    fun join() = compatibleBlockingTest {
        val task = SingleTask()

        assertTrue(task.run { true })
        task.join()
        assertTrue(task.run { true })
        task.join()
        assertFalse(task.isActive)
    }

    @Test
    fun async() = compatibleBlockingTest {
        val task = SingleTask()
        assertEquals(2, task.async { 2 }.await())
        assertFalse(task.isActive)
    }

    @Test
    fun launch() = compatibleBlockingTest {
        val task = SingleTask()
        val channel = Channel<Int>()
        val job = task.launch {
            channel.send(2)
        }

        assertEquals(2, channel.receive())
        job.join()
        assertFalse(task.isActive)
    }

    @Test
    fun async_cancel() = compatibleBlockingTest {
        val task = SingleTask(FlexibleThreadPoolDispatcher.Network)
        val job = task.async {
            delay(10000)
            yield()
            2
        }

        try {
            assertEquals(job, task.currentTask)
            task.cancel()
            val result = job.await()
            fail("Failed task.isCancelled='${task.isCancelled}' job.isCancelled='${job.isCancelled}' job.isCompleted='${job.isCompleted}' result='$result'")
        } catch (e: CancellationException) {
            // ok
        }
        assertFalse(task.isActive)
    }
}