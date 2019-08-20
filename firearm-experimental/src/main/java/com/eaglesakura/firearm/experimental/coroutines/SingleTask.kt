package com.eaglesakura.firearm.experimental.coroutines

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.eaglesakura.armyknife.android.extensions.onUiThread
import com.eaglesakura.armyknife.runtime.extensions.job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
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
class SingleTask constructor(
    private val taskName: String,
    private val dispatcher: CoroutineDispatcher
) {
    constructor(dispatcher: CoroutineDispatcher = Dispatchers.IO) : this(
        taskName = "SingleTask",
        dispatcher = dispatcher
    )

    private val lock = ReentrantLock()

    internal var scope: CoroutineScope? = null

    /**
     * Task cancel.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun cancel() {
        val scope = lock.withLock { this@SingleTask.scope }
        Log.i("SingleTask", "try cancel() name='$taskName', scope=$scope")
        scope?.cancel(CancellationException("Cancel from SingleTask"))
    }

    /**
     * Task cancel and await.
     */
    suspend fun cancelAndJoin() {
        val scope = lock.withLock { this@SingleTask.scope }
        Log.i("SingleTask", "try cancelAndJoin() name='$taskName', scope=$scope")

        scope?.cancel(CancellationException("cancelAndJoin() name='$taskName'"))
        scope?.coroutineContext?.job?.join()
    }

    /**
     * Join a task.
     */
    suspend fun join() {
        val scope = lock.withLock { this@SingleTask.scope }
        Log.i("SingleTask", "try join() name='$taskName', scope=$scope")
        scope?.coroutineContext?.also { context ->
            context.job.join()
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
        val nextScope = (GlobalScope + Job())
        val executeTaskName = makeTaskName(name)
        var completed = false
        try {
            withContext(Dispatchers.Main) {
                runningImpl.value = (runTasks.incrementAndGet() > 0)
            }

            cancelAndJoin()
            lock.withLock {
                scope = nextScope
            }

            if (nextScope != this.scope) {
                throw CancellationException("Task conflict, name='$executeTaskName', scope=$nextScope")
            }

            return withContext(nextScope.coroutineContext) {
                withContext(dispatcher) {
                    Log.i("SingleTask", "start task name='$executeTaskName'")
                    block(this).also {
                        yield()
                        completed = true
                    }
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
                if (completed) {
                    Log.i("SingleTask", "completed name='$executeTaskName' tasks='$runTasks', scope=$nextScope")
                } else {
                    Log.i(
                        "SingleTask",
                        "conflict or canceled, name='$executeTaskName' tasks='$runTasks', scope=$nextScope"
                    )
                }
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