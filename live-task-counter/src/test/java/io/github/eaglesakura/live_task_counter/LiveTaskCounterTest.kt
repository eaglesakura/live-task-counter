package io.github.eaglesakura.live_task_counter

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eaglesakura.armyknife.android.junit4.extensions.instrumentationBlockingTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveTaskCounterTest {

    @Test
    fun init_ui() = instrumentationBlockingTest(Dispatchers.Main) {
        LiveTaskCounter()
    }

    @Test
    fun init_background() = instrumentationBlockingTest {
        LiveTaskCounter()
    }

    @Test
    fun withCount() = instrumentationBlockingTest {
        listOf(
            Dispatchers.Main,
            Dispatchers.Default,
            Dispatchers.IO
        ).forEach { dispatcher ->
            Log.d("LiveTaskCounterTest", "dispatcher=$dispatcher")

            withContext(dispatcher) {
                val counter = LiveTaskCounter()
                assertEquals(0, counter.count)

                val tasks = mutableListOf<Job>().also { list ->
                    repeat(100) {
                        list.add(
                            launch {
                                counter.withCount {
                                    delay(1000)
                                }
                            }
                        )
                    }
                }

                delay(500)
                assertEquals(tasks.size, counter.count)
                assertTrue(counter.isNotEmpty)
                assertFalse(counter.isEmpty)

                tasks.joinAll()
                assertEquals(0, counter.count)
                assertFalse(counter.isNotEmpty)
                assertTrue(counter.isEmpty)
            }
        }
    }

    @Test
    fun allOf() = instrumentationBlockingTest {
        val readCounter = LiveTaskCounter()
        val writeCounter = LiveTaskCounter()

        val allCounter = LiveTaskCounter.allOf(readCounter, writeCounter)

        assertNotNull(allCounter.value)

        val readJob = launch {
            readCounter.withCount {
                delay(1000)
            }
        }
        val writeJob = launch {
            writeCounter.withCount {
                delay(1000)
            }
        }

        delay(100)

        assertEquals(1, readCounter.count)
        assertEquals(1, writeCounter.count)
        assertEquals(2, allCounter.value!!.count)
        assertEquals(
            2,
            allCounter.value!!.version
        )

        readJob.join()
        writeJob.join()
        withContext(Dispatchers.Main) {
            assertEquals(0, readCounter.count)
            assertEquals(0, writeCounter.count)
            assertEquals(4, allCounter.value!!.version)
        }
    }
}
