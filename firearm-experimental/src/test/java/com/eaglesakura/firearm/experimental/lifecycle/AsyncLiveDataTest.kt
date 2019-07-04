package com.eaglesakura.firearm.experimental.lifecycle

import androidx.lifecycle.Observer
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eaglesakura.armyknife.android.junit4.extensions.compatibleBlockingTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AsyncLiveDataTest {

    @Test
    fun onInit() = compatibleBlockingTest(Dispatchers.Main) {
        val liveData = AsyncLiveData<String>(Dispatchers.Main) {
            delay(100)
            "OK"
        }

        delay(1000)
        assertNull(liveData.value)
    }

    @Test
    fun onActive() = compatibleBlockingTest(Dispatchers.Main) {
        val liveData = AsyncLiveData<String>(Dispatchers.Main) {
            delay(100)
            "OK"
        }

        liveData.observeForever {
            assertEquals("OK", it)
        }

        delay(1000)
        assertEquals("OK", liveData.value)
    }

    @Test
    fun onActive_cancel() = compatibleBlockingTest(Dispatchers.Main) {
        val liveData = AsyncLiveData<String>(Dispatchers.Main) {
            delay(1000)
            "OK"
        }

        val observer = Observer<String> { assertNull(it) }
        liveData.observeForever(observer) // active
        assertNotNull(liveData.scope)
        delay(100)
        liveData.removeObserver(observer) // inactive
        assertNull(liveData.scope)
        delay(1000)
        assertNull(liveData.value)
    }
}