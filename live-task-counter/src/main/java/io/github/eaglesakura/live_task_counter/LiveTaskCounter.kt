package io.github.eaglesakura.live_task_counter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

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
@Suppress("unused")
class LiveTaskCounter : LiveData<LiveTaskCounter.Snapshot>(Snapshot(Date(), 0, 0)) {

    private val versionImpl = AtomicLong()

    private val countImpl = AtomicInteger()

    /**
     * Current count.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val count: Int
        get() = (this.value?.count ?: 0)

    /**
     * has countImpl
     */
    @Suppress("unused")
    val isNotEmpty: Boolean
        get() = this.count > 0

    /**
     * not have countImpl.
     */
    @Suppress("unused")
    @Deprecated("use .isEmpty", ReplaceWith("isEmpty"))
    val empty: Boolean
        get() = this.count == 0

    /**
     * not have countImpl.
     */
    @Suppress("unused")
    val isEmpty: Boolean
        get() = this.count == 0

    /**
     * Run task and countImpl.
     *
     * when start this function, then increment this LiveData.
     * when finally this function, then decrement this LiveData.
     */
    @Suppress("unused")
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
        @Suppress("unused")
        val isNotEmpty: Boolean
            get() = count > 0

        @Suppress("unused")
        @Deprecated("use .isEmpty", ReplaceWith("isEmpty"))
        val empty: Boolean
            get() = count == 0

        @Suppress("unused")
        val isEmpty: Boolean
            get() = count == 0
    }

    companion object {
        /**
         * Check all task counter.
         */
        @Suppress("unused")
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

            tasks.forEach {
                result.addSource(it, sync)
            }
            return result
        }
    }
}
