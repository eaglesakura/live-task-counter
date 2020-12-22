package com.eaglesakura.firearm.experimental.coroutines

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.eaglesakura.armyknife.android.extensions.runBlockingOnUiThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Count parallel tasks with LiveData.
 *
 * e.g.)
 *
 * val counter = LiveTaskCounter()
 *
 * init {
 *      counter.observeForever {
 *          val snapshot = it ?: return@observeForever
 *          if(snapshot.empty) {
 *              // stop tasks
 *          }
 *      }
 * }
 *
 * suspend fun foo() {
 *      counter.withCount {
 *          // do something
 *      }
 * }
 *
 * suspend fun bar() {
 *      counter.withCount {
 *          // do something
 *      }
 * }
 *
 */
class LiveTaskCounter : LiveData<LiveTaskCounter.Snapshot>(Snapshot(Date(), 0, 0)) {

    private val versionImpl = AtomicLong()

    private val countImpl = AtomicInteger()

    /**
     * Current count.
     */
    val count: Int
        get() = (this.value?.count ?: 0)

    /**
     * has countImpl
     */
    val isNotEmpty: Boolean
        get() = this.count > 0

    /**
     * not have countImpl.
     */
    val empty: Boolean
        get() = this.count == 0

    /**
     * Run task and countImpl.
     *
     * when start this function, then increment this LiveData.
     * when finally this function, then decrement this LiveData.
     */
    suspend fun <T> withCount(action: suspend () -> T): T {
        try {
            withContext(Dispatchers.Main + NonCancellable) {
                this@LiveTaskCounter.value = Snapshot(
                    version = versionImpl.incrementAndGet(),
                    date = Date(),
                    count = countImpl.incrementAndGet()
                )
            }
            return action()
        } finally {
            withContext(Dispatchers.Main + NonCancellable) {
                this@LiveTaskCounter.value = Snapshot(
                    version = versionImpl.incrementAndGet(),
                    date = Date(),
                    count = countImpl.decrementAndGet()
                )
            }
        }
    }

    /**
     * Current context.
     */
    data class Snapshot(
        val date: Date,
        val version: Long,
        val count: Int
    ) {
        val isNotEmpty: Boolean
            get() = count > 0

        val empty: Boolean
            get() = count == 0
    }

    companion object {
        /**
         * Check all task counter.
         */
        fun allOf(vararg tasks: LiveTaskCounter): LiveData<Snapshot> {
            require(tasks.isNotEmpty()) {
                "tasks is empty"
            }
            val result = MediatorLiveData<Snapshot>()
            val sync: (Snapshot) -> Unit = {
                result.value = Snapshot(
                    date = Date(),
                    count = tasks.sumBy { it.value!!.count },
                    version = tasks.sumBy { it.value!!.version.toInt() }.toLong()
                )
            }

            return runBlockingOnUiThread {
                tasks.forEach {
                    result.addSource(it, sync)
                }
                // activate
                result.observeForever {}

                result
            }
        }
    }
}