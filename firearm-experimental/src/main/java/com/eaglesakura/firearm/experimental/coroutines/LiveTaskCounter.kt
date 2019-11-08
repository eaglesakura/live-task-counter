package com.eaglesakura.firearm.experimental.coroutines

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

/**
 * Count parallel tasks with LiveData.
 */
class LiveTaskCounter : LiveData<Int>() {

    private val requestPermits = AtomicInteger()

    override fun getValue(): Int {
        return super.getValue() ?: 0
    }

    /**
     * has count
     */
    val isNotEmpty: Boolean
        get() = this.value > 0

    /**
     * not have count.
     */
    val empty: Boolean
        get() = this.value == 0

    /**
     * Run task and count.
     *
     * when start this function, then increment this LiveData.
     * when finally this function, then decrement this LiveData.
     */
    suspend fun <T> withCount(action: suspend () -> T): T {
        try {
            withContext(Dispatchers.Main + NonCancellable) {
                this@LiveTaskCounter.value = requestPermits.incrementAndGet()
            }
            return action()
        } finally {
            withContext(Dispatchers.Main + NonCancellable) {
                this@LiveTaskCounter.value = requestPermits.decrementAndGet()
            }
        }
    }
}