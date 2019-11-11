package com.eaglesakura.firearm.experimental.coroutines

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
                        list.add(launch {
                            counter.withCount {
                                delay(1000)
                            }
                        })
                    }
                }

                delay(500)
                assertEquals(tasks.size, counter.count)
                assertTrue(counter.isNotEmpty)
                assertFalse(counter.empty)

                tasks.joinAll()
                assertEquals(0, counter.count)
                assertFalse(counter.isNotEmpty)
                assertTrue(counter.empty)
            }
        }
    }
}