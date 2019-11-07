package com.eaglesakura.firearm.experimental.widgets

import android.view.View
import com.eaglesakura.firearm.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * launch async task for view build.
 * e.g.
 *
 * val view = findViewById(...)
 * view.launchViewBuilder { view ->
 *      // do building in Coroutines.
 * }
 */
fun <T : View> T.launchViewBuilder(
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
    block: suspend (view: T) -> Unit
): Job {
    (getTag(R.id.firearm_key_view_binding_job) as? Job)?.also { job ->
        job.cancel()
    }

    val view = this
    val job = ScopeImpl.launch(dispatcher) {
        block(view)
    }
    setTag(R.id.firearm_key_view_binding_job, job)
    return job
}

private object ScopeImpl : CoroutineScope {
    override val coroutineContext: CoroutineContext =
        SupervisorJob() + Dispatchers.Main.immediate
}