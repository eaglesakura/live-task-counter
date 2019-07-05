package com.eaglesakura.firearm.experimental.viewmodel

import android.app.Activity
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.transaction
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eaglesakura.armyknife.android.junit4.extensions.instrumentationBlockingTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.reflect.KClass

@RunWith(AndroidJUnit4::class)
class ViewModelSessionTest {

    @Test
    fun refresh_Activity() = instrumentationBlockingTest(Dispatchers.Main) {
        val activity = makeActivity(AppCompatActivity::class)
        val session = ViewModelSession<Activity>()
        session.refresh(activity)

        assertEquals(activity, session.owner.value)
        assertEquals(activity, session.context.value)
        assertEquals(activity, session.lifecycleOwner.value)
        assertTrue(session.coroutineScope.value!!.isActive)
    }

    @Test
    fun refresh_Fragment() = instrumentationBlockingTest(Dispatchers.Main) {
        val fragment = makeFragment(Fragment::class)

        val session = ViewModelSession<Fragment>()
        session.refresh(fragment)

        assertEquals(fragment, session.owner.value)
        assertEquals(fragment.requireContext(), session.context.value)
        assertEquals(fragment, session.lifecycleOwner.value)
        assertTrue(session.coroutineScope.value!!.isActive)
    }

    @Test
    fun clear() = instrumentationBlockingTest(Dispatchers.Main) {
        val activity = makeActivity(AppCompatActivity::class)
        val session = ViewModelSession<Activity>()
        session.refresh(activity)
        val scope = session.coroutineScope.value!!

        session.clear()

        assertNull(session.owner.value)
        assertNull(session.context.value)
        assertNull(session.lifecycleOwner.value)
        assertNull(session.coroutineScope.value)
        assertNull(session.value)
        assertFalse(scope.isActive)
    }
}

/**
 * Make testing activity.
 */
suspend fun <T : FragmentActivity> makeActivity(clazz: KClass<T>): T {
    return withContext(Dispatchers.Default) {
        val scenario = ActivityScenario.launch(clazz.java)
        val channel = Channel<T>()
        scenario.onActivity { activity ->
            GlobalScope.launch { channel.send(activity) }
        }
        channel.receive()
    }
}

/**
 * Make testing activity.
 */
suspend fun makeActivity(): AppCompatActivity = makeActivity(AppCompatActivity::class)

/**
 * Make testing fragment.
 */
suspend fun <A : FragmentActivity, F : Fragment> makeFragment(activityClass: KClass<A>, @IdRes containerViewId: Int, factory: (activity: A) -> F): F {
    return withContext(Dispatchers.Default) {
        val scenario = ActivityScenario.launch(activityClass.java)
        val channel = Channel<F>()
        scenario.onActivity { activity ->
            val fragment = factory(activity)
            activity.supportFragmentManager.transaction {
                if (containerViewId == 0) {
                    add(fragment, fragment.javaClass.name)
                } else {
                    add(containerViewId, fragment, fragment.javaClass.name)
                }
            }
            GlobalScope.launch { channel.send(fragment) }
        }
        channel.receive()
    }
}

/**
 * Make testing fragment without View.
 */
suspend fun <A : FragmentActivity, F : Fragment> makeFragment(activityClass: KClass<A>, factory: (activity: A) -> F): F {
    return makeFragment(activityClass, 0x00, factory)
}

/**
 * Make testing fragment with View.
 */
suspend fun <A : FragmentActivity, F : Fragment> makeFragment(activityClass: KClass<A>, @IdRes containerViewId: Int, fragmentClass: KClass<F>): F {
    @Suppress("MoveLambdaOutsideParentheses")
    return makeFragment(activityClass, 0x00, {
        fragmentClass.java.newInstance()
    })
}

/**
 * Make testing fragment without View.
 */
suspend fun <A : FragmentActivity, F : Fragment> makeFragment(activityClass: KClass<A>, fragmentClass: KClass<F>): F {
    @Suppress("MoveLambdaOutsideParentheses")
    return makeFragment(activityClass, 0x00, {
        fragmentClass.java.newInstance()
    })
}

/**
 * Make testing fragment with View.
 */
suspend fun <F : Fragment> makeFragment(@IdRes containerViewId: Int, factory: (activity: AppCompatActivity) -> F): F {
    return makeFragment(AppCompatActivity::class, containerViewId, factory)
}

/**
 * Make testing fragment without View.
 */
suspend fun <F : Fragment> makeFragment(factory: (activity: AppCompatActivity) -> F): F {
    return makeFragment(AppCompatActivity::class, 0x00, factory)
}

/**
 * Make testing fragment without View.
 */
suspend fun <F : Fragment> makeFragment(fragmentClass: KClass<F>): F {
    @Suppress("MoveLambdaOutsideParentheses")
    return makeFragment(AppCompatActivity::class, 0x00, { fragmentClass.java.newInstance() })
}
