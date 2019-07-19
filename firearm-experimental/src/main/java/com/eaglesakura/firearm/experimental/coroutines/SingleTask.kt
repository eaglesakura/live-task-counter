package com.eaglesakura.firearm.experimental.coroutines

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.eaglesakura.armyknife.runtime.extensions.job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Single(Exclusive) task.
 * If task is running, then cancel old task, And start new task.
 *
 * val task: SingleTask = ... // SingleTask
 *
 * suspend fun exampleA() {
 *      task.run {
 *          // if running old task.
 *          // then cancel it.
 *      }
 * }
 *
 * suspend fun exampleB() {
 *      task.run {
 *          // if running old task.
 *          // then cancel it.
 *          // execute heavy
 *      }
 * }
 */
class SingleTask(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val lock = ReentrantLock()

    internal var scope: CoroutineScope? = null

    /**
     * Task cancel.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun cancel() {
        lock.withLock {
            scope?.cancel(CancellationException("Cancel from SingleTask"))
        }
    }

    suspend fun cancelAndJoin() {
        val task = lock.withLock { scope?.coroutineContext ?: return }
        task.job.cancelAndJoin()
    }

    /**
     * Join a task.
     */
    suspend fun join() {
        lock.withLock { scope?.coroutineContext ?: return }.job.join()
    }

    /**
     * current running tasks.
     */
    private val runTasks = AtomicInteger()

    private val runningImpl = MutableLiveData<Boolean>().also { it.value = false }

    /**
     * Task running now.
     */
    val running: LiveData<Boolean>
        get() = runningImpl

    /**
     * Run single task.
     */
    suspend fun <T> run(block: suspend CoroutineScope.() -> T): T {
        val nextScope = (GlobalScope + Job())
        try {
            withContext(Dispatchers.Main) {
                runningImpl.value = (runTasks.incrementAndGet() > 0)
                Log.i("SingleTask", "run tasks='$runTasks'")
            }

            cancelAndJoin()
            lock.withLock {
                scope = nextScope
            }

            if (nextScope != this.scope) {
                throw CancellationException("Task conflict.")
            }

            return withContext(nextScope.coroutineContext) {
                withContext(dispatcher) {
                    block()
                }
            }
        } finally {
            lock.withLock {
                nextScope.cancel()
                if (nextScope == scope) {
                    scope = null
                }
            }
            withContext(Dispatchers.Main) {
                runningImpl.value = (runTasks.decrementAndGet() > 0)
                Log.i("SingleTask", "finish run tasks='$runTasks'")
            }
        }
    }

    /**
     * Run single task with timeout.
     */
    suspend fun <T> withTimeout(
        time: Long,
        timeUnit: TimeUnit,
        block: suspend CoroutineScope.() -> T
    ): T {
        return kotlinx.coroutines.withTimeout(timeUnit.toMillis(time)) {
            run(block)
        }
    }

    override fun toString(): String {
        return "SingleTask(dispatcher=$dispatcher, currentTask=$scope)"
    }
}