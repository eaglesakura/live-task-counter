package com.eaglesakura.firearm.experimental.coroutines

import androidx.lifecycle.Lifecycle
import com.eaglesakura.armyknife.android.extensions.with
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.coroutineContext

/**
 * Single(Exclusive) task.
 * If task is running, then cancel old task, And start new task.
 *
 * val task: SingleTask = ... // SingleTask
 * task.
 */
class SingleTask(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val lock = ReentrantLock()

    internal var currentTask: Job? = null

    /**
     * Task is active
     * If task not started, then return `false`,
     */
    val isActive: Boolean
        get() = currentTask?.isActive ?: false

    /**
     * Task was cancel.
     * If task not started, then return `false`,
     */
    val isCancelled: Boolean
        get() = currentTask?.isCancelled ?: false

    /**
     * Task cancel.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun cancel() {
        lock.withLock {
            currentTask?.cancel()
        }
    }

    suspend fun cancelAndJoin() {
        val task = lock.withLock { currentTask ?: return }
        task.cancelAndJoin()
    }

    /**
     * Join a task.
     */
    suspend fun join() = currentTask?.join()

    /**
     * Run single task.
     */
    suspend fun <T> run(block: suspend CoroutineScope.() -> T): T {
        cancelAndJoin()
        val channel = Channel<T>()
        currentTask = GlobalScope.async(coroutineContext) {
            try {
                channel.send(block())
            } catch (e: CancellationException) {
                channel.cancel(e)
            }
        }
        return channel.receive()
    }

    /**
     * Run single task with Lifecycle scope.
     * When lifecycle.onDestroy, then cancel this task.
     */
    suspend fun <T> run(lifecycle: Lifecycle, block: suspend CoroutineScope.() -> T): T {
        return run {
            coroutineContext.with(lifecycle)
            withContext(dispatcher) {
                block()
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

    /**
     * Launch single task with lifecycle.
     */
    fun launch(block: suspend CoroutineScope.() -> Unit): Job = this.async(block)

    /**
     * Launch single task with lifecycle.
     */
    fun launch(lifecycle: Lifecycle, block: suspend CoroutineScope.() -> Unit): Job =
        this.async(lifecycle, block)

    /**
     * Async single task with lifecycle.
     */
    fun <T> async(block: suspend CoroutineScope.() -> T): Deferred<T> {
        return lock.withLock {
            val oldTask = currentTask
            cancel()
            val result = GlobalScope.async(Dispatchers.Main) {
                oldTask?.join()
                withContext(dispatcher) {
                    block()
                }
            }
            currentTask = result
            result
        }
    }

    /**
     * Async single task with lifecycle.
     */
    fun <T> async(lifecycle: Lifecycle, block: suspend CoroutineScope.() -> T): Deferred<T> =
        async {
            coroutineContext.with(lifecycle)
            block()
        }

    override fun toString(): String {
        return "SingleTask(dispatcher=$dispatcher, currentTask=$currentTask)"
    }
}