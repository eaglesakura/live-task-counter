package com.eaglesakura.firearm.experimental.viewmodel

import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.util.Log
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.OnLifecycleEvent
import com.eaglesakura.armyknife.android.extensions.assertUIThread
import com.eaglesakura.armyknife.android.extensions.findInterface
import com.eaglesakura.armyknife.runtime.extensions.instanceOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

/**
 * ViewModel owner controller.
 *
 * e.g.)
 *
 * class ExampleViewModel : ViewModel() {
 *      val session = ViewModelSession<Fragment>()
 *
 *      val context: LiveData<Context>
 *          get() = session.context
 *
 *      private val observeSessionToken = Observer<ViewModelSession.Token<Fragment>> {
 *          val token = it ?: return
 *          token.launch(Dispatchers.IO) {
 *              // do initialize.
 *          }
 *      }
 * }
 *
 * class ExampleFragment : Fragment() {
 *      lateinit var viewModel: ExampleViewModel
 *
 *      fun onCreateView(...) {
 *          viewModel.session.refresh(this)
 *      }
 * }
 */
class ViewModelSession<T> : LiveData<ViewModelSession.Token<T>>(), CoroutineScope {
    private val ownerImpl = MutableLiveData<T>()

    private val contextImpl = MutableLiveData<Context>()

    private val lifecycleOwnerImpl = MutableLiveData<LifecycleOwner>()

    private val applicationImpl = MutableLiveData<Application>()

    private val coroutineScopeImpl = MutableLiveData<CoroutineScope>()

    private val linkedLiveData: MutableSet<LiveData<*>> = mutableSetOf()

    override val coroutineContext: CoroutineContext
        get() = coroutineScope.value?.coroutineContext
            ?: throw IllegalStateException("Owner not attached, You should call ViewModelSession.refresh(owner)")

    /**
     * Owner object data.
     */
    @Suppress("unused")
    val owner: LiveData<T>
        get() = ownerImpl

    /**
     * Context live data.
     */
    @Suppress("unused")
    val context: LiveData<Context>
        get() = contextImpl

    /**
     * ApplicationContext live data.
     * this value will not clear on `clear()` function.
     */
    @Suppress("unused")
    val application: LiveData<Application>
        get() = applicationImpl

    /**
     * Lifecycle Owner live data.
     */
    @Suppress("unused")
    val lifecycleOwner: LiveData<LifecycleOwner>
        get() = lifecycleOwnerImpl

    /**
     * CoroutineScope active this session.
     *
     * when onDestroy(or call this.clear() func) event, then cancel this.
     */
    @Suppress("unused")
    val coroutineScope: LiveData<CoroutineScope>
        get() = coroutineScopeImpl

    /**
     * Refresh new session value.
     */
    @Suppress("unused")
    @UiThread
    fun refresh(owner: T) {
        assertUIThread()

        refresh(
            owner = owner,
            context = when (owner) {
                is Fragment -> owner.requireContext()
                is Context -> owner
                else -> throw IllegalArgumentException("Not supported owner context, $owner")
            },
            lifecycleOwner = when (owner) {
                is LifecycleOwner -> owner
                else -> throw IllegalArgumentException("Not supported owner lifecycle, $owner")
            }
        )
    }

    /**
     * Refresh session with raw values.
     */
    @UiThread
    fun refresh(owner: T, context: Context, lifecycleOwner: LifecycleOwner) {
        assertUIThread()
        clear()

        require(lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
            "owner($owner) is destroyed"
        }

        val token = Token(
            owner = owner,
            lifecycleOwner = lifecycleOwner,
            context = context
        )
        coroutineScopeImpl.value = token
        ownerImpl.value = token.owner
        contextImpl.value = token.context
        lifecycleOwnerImpl.value = token.lifecycleOwner
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        if (applicationImpl.value == null) {
            applicationImpl.value = context.applicationContext as Application
        }

        // refresh completed
        this.value = token

        // active link
        liveDataToActive()
    }

    /**
     * remove this session.
     */
    @UiThread
    fun clear() {
        assertUIThread()
        Log.d("ViewModelSession", "clear session $this, owner=${owner.value}")

        liveDataToInactive()

        ownerImpl.value?.also {
            ownerImpl.value = null
        }
        contextImpl.value?.also {
            contextImpl.value = null
        }
        lifecycleOwnerImpl.value?.also {
            it.lifecycle.removeObserver(lifecycleObserver)
            lifecycleOwnerImpl.value = null
        }
        coroutineScopeImpl.value?.also {
            coroutineScopeImpl.value = null
        }

        value?.also { token ->
            token.close()
            value = null
        }
    }

    @Suppress("unused")
    private val lifecycleObserver = object : LifecycleObserver {
        @Keep
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            Log.d("ViewModelSession", "${this@ViewModelSession} auto clear.")
            clear()
        }
    }

    private val resources: Resources
        get() = value?.resources
            ?: throw IllegalStateException("Owner not attached, You should call ViewModelSession.refresh(owner)")

    private val theme: Resources.Theme
        get() = value?.theme
            ?: throw IllegalStateException("Owner not attached, You should call ViewModelSession.refresh(owner)")

    /**
     * Returns string resource from current session's owner.
     * when not initialized(or cleared) then throw IllegalStateException.
     */
    @Suppress("unused")
    @Throws(IllegalStateException::class)
    fun getString(@StringRes id: Int): String = resources.getString(id)

    /**
     * Returns string resource from current session's owner.
     * when not initialized(or cleared) then throw IllegalStateException.
     */
    @Suppress("unused")
    @Throws(IllegalStateException::class)
    fun getString(@StringRes id: Int, vararg arg: Any): String = resources.getString(id, *arg)

    /**
     * Returns drawable from current session's owner.
     * when not initialized(or cleared) then throw IllegalStateException.
     */
    @Suppress("unused")
    @Throws(IllegalStateException::class)
    fun getDrawable(@DrawableRes id: Int) = ResourcesCompat.getDrawable(resources, id, theme)

    /**
     * Returns color from current session's owner.
     * when not initialized(or cleared) then throw IllegalStateException.
     */
    @Suppress("unused")
    fun getColor(@ColorRes id: Int) = ResourcesCompat.getColor(resources, id, theme)

    /**
     * Find interface from owner.
     * when not found, then returns null.
     *
     * @see Fragment.findInterface
     */
    @Suppress("unused")
    fun <T : Any> findInterface(clazz: KClass<T>): T? {
        value?.also {
            return it.findInterface(clazz)
        }
        throw IllegalStateException("Owner not attached, You should call ViewModelSession.refresh(owner)")
    }

    /**
     * Session link to target LiveData.
     * when this ViewModelSession refresh, then `target` LiveData change to active.
     *
     * e.g.)
     * class ExampleViewModel : ViewModel {
     *
     *      val session = ViewModelSession<Fragment>
     *
     *      // MediatorLiveData work on active only.
     *      val message = MediatorLiveData<String>().also { result ->
     *                  result.addSource(session) {
     *                      result.value = session.value?.toString()
     *                  }
     *                }
     * }
     *
     * init {
     *      session.linkTo(message)
     * }
     *
     * fun refresh(fragment: Fragment) {
     *      session.refresh(fragment)
     *      // this.message changed to Active!
     *      require(message.value != null)
     * }
     */
    @UiThread
    fun linkTo(target: LiveData<*>): ViewModelSession<T> {
        assertUIThread()

        this.linkedLiveData.add(target)
        if (value != null) {
            target.observeForever(activeLiveDataObserver)
        }
        return this
    }

    /**
     * Delete link
     */
    @UiThread
    fun unlink(target: LiveData<*>): ViewModelSession<T> {
        assertUIThread()

        if (this.linkedLiveData.remove(target)) {
            target.removeObserver(activeLiveDataObserver)
        }
        return this
    }

    private val activeLiveDataObserver: Observer<Any> = Observer {}

    private fun liveDataToActive() {
        linkedLiveData.forEach { it.observeForever(activeLiveDataObserver) }
    }

    private fun liveDataToInactive() {
        linkedLiveData.forEach { it.removeObserver(activeLiveDataObserver) }
    }

    /**
     * Current session token.
     */
    class Token<T>(
        val owner: T,
        val context: Context,
        val lifecycleOwner: LifecycleOwner
    ) : CoroutineScope, LifecycleOwner {
        /**
         * Token unique id.
         */
        @Suppress("unused")
        val id: Long = System.currentTimeMillis()

        override val coroutineContext: CoroutineContext =
            (Dispatchers.Default + Job())

        override fun getLifecycle(): Lifecycle = lifecycleOwner.lifecycle

        /**
         * Access to context resources.
         */
        @Suppress("unused")
        val resources: Resources
            get() = context.resources

        /**
         * Access to context theme.
         */
        @Suppress("unused")
        val theme: Resources.Theme
            get() = context.theme

        /**
         * Access to application context.
         */
        @Suppress("unused")
        val application: Application
            get() = context.applicationContext as Application

        internal fun close() {
            coroutineContext.cancel(CancellationException("'$this' is cleared"))
        }

        /**
         * Returns string resource from current session's owner.
         * when not initialized(or cleared) then throw IllegalStateException.
         */
        @Suppress("unused")
        fun getString(@StringRes id: Int): String = resources.getString(id)

        /**
         * Returns string resource from current session's owner.
         * when not initialized(or cleared) then throw IllegalStateException.
         */
        @Suppress("unused")
        fun getString(@StringRes id: Int, vararg arg: Any): String = resources.getString(id, *arg)

        /**
         * Returns drawable from current session's owner.
         * when not initialized(or cleared) then throw IllegalStateException.
         */
        @Suppress("unused")
        fun getDrawable(@DrawableRes id: Int) = ResourcesCompat.getDrawable(resources, id, theme)

        /**
         * Returns color from current session's owner.
         * when not initialized(or cleared) then throw IllegalStateException.
         */
        @Suppress("unused")
        fun getColor(@ColorRes id: Int) = ResourcesCompat.getColor(resources, id, theme)

        /**
         * Find interface from owner.
         * when not found, then returns null.
         *
         * @see Fragment.findInterface
         */
        fun <T : Any> findInterface(clazz: KClass<T>): T? {
            val check: Any = this.owner as Any
            return when (owner) {
                check.instanceOf(clazz) -> {
                    @Suppress("UNCHECKED_CAST")
                    check as? T
                }
                check is Fragment -> {
                    (check as Fragment).findInterface(clazz)
                }
                else -> null
            }
        }

        override fun toString(): String {
            return "Token(id=$id, owner=$owner)"
        }
    }
}