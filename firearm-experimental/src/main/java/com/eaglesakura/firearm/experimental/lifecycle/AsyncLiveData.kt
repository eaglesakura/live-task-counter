package com.eaglesakura.firearm.experimental.lifecycle

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.coroutines.CoroutineContext

/**
 * Update value on async context.
 *
 * val liveData = AsyncLiveData<String>(Dispatchers.Main) {
 *      // call onActive().
 * }
 */
class AsyncLiveData<T>(
    private val context: CoroutineContext,
    private val factory: suspend (self: LiveData<T>) -> T?
) : LiveData<T>() {

    internal var scope: CoroutineScope? = null

    override fun onActive() {
        scope = (GlobalScope + Job())
        scope?.launch(context) {
            val value = factory(this@AsyncLiveData)
            yield()
            withContext(Dispatchers.Main) {
                this@AsyncLiveData.value = value
            }
        }
    }

    override fun onInactive() {
        scope?.cancel()
        scope = null
    }
}