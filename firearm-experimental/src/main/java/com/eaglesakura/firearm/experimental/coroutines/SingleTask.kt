package com.eaglesakura.firearm.experimental.coroutines

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.eaglesakura.armyknife.android.extensions.onUiThread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.CoroutineContext

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
class SingleTask constructor(
    private val taskName: String,
    private val dispatcher: CoroutineDispatcher
) {
    constructor(dispatcher: CoroutineDispatcher = Dispatchers.IO) : this(
        taskName = "SingleTask",
        dispatcher = dispatcher
    )

    private val lock = ReentrantLock()

    @VisibleForTesting
    internal var job: Job? = null

    /**
     * Task cancel.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun cancel() {
        lock.withLock {
            job?.cancel(CancellationException("Cancel from SingleTask"))
            job = null
        }
    }

    /**
     * Task cancel and await.
     */
    suspend fun cancelAndJoin() {
        cancel()
        join()
    }

    /**
     * Join a task.
     */
    suspend fun join() {
        try {
            while (runTasks.get() > 0) {
                delay(1)
            }
        } finally {
            syncRunning()
        }
    }

    /**
     * current running tasks.
     */
    private val runTasks = AtomicInteger()

    private val runningImpl = MutableLiveData<Boolean>().also {
        // Initial value, on any thread.
        if (onUiThread) {
            it.value = false
        } else {
            it.postValue(false)
            while (it.value == null) {
            }
        }
    }

    private fun syncRunning() {
        if (onUiThread) {
            runningImpl.value = runTasks.get() > 0
        } else {
            runBlocking(Dispatchers.Main + NonCancellable) {
                syncRunning()
            }
        }
    }

    /**
     * Task running now.
     */
    val running: LiveData<Boolean>
        get() = runningImpl

    /**
     * Task is running.
     */
    val isRunning: Boolean
        get() = runTasks.get() > 0

    private fun makeTaskName(name: String): String {
        return if (name.isEmpty()) {
            taskName
        } else {
            "$taskName/$name"
        }
    }

    /**
     * Run single task.
     */
    suspend fun <T> run(block: suspend CoroutineScope.() -> T): T = run("", block)

    /**
     * Run single task with name.
     */
    suspend fun <T> run(name: String, block: suspend CoroutineScope.() -> T): T {
        try {
            runTasks.incrementAndGet()
            syncRunning()

            val executeTaskName = makeTaskName(name)
            val deferred = lock.withLock {
                val oldTask = this.job
                CoroutineScopeImpl.async(dispatcher) {
                    try {
                        oldTask?.cancelAndJoin()
                        yield()
                        val result = block(this)
                        Log.i(
                            "SingleTask",
                            "completed name='$executeTaskName' tasks='$runTasks'"
                        )
                        result
                    } catch (e: CancellationException) {
                        Log.i(
                            "SingleTask",
                            "conflict or canceled, name='$executeTaskName' tasks='$runTasks'"
                        )
                        throw e
                    } finally {
                    }
                }.also {
                    this.job = it
                }
            }

            try {
                return deferred.await()
            } finally {
                lock.withLock {
                    if (deferred == job) {
                        job = null
                    }
                }
            }
        } finally {
            runTasks.decrementAndGet()
            syncRunning()
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
        return "SingleTask(name=$taskName, dispatcher=$dispatcher, currentTask=$job)"
    }

    private object CoroutineScopeImpl : CoroutineScope {
        override val coroutineContext: CoroutineContext = (SupervisorJob() + Dispatchers.Default)
    }
}