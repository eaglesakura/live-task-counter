package com.eaglesakura.firearm.experimental.coroutines

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eaglesakura.armyknife.android.junit4.extensions.instrumentationBlockingTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
                repeat(100) {
                    launch {
                        counter.withCount {
                            delay(1000)
                        }
                    }
                }
                delay(100)
                assertEquals(100, counter.count)
                assertTrue(counter.isNotEmpty)
                assertFalse(counter.empty)

                delay(2000)
                assertEquals(0, counter.count)
                assertFalse(counter.isNotEmpty)
                assertTrue(counter.empty)
            }
        }
    }
}