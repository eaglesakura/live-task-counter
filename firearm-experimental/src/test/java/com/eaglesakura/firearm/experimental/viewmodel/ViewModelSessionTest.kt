package com.eaglesakura.firearm.experimental.viewmodel

import android.app.Activity
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.eaglesakura.armyknife.android.extensions.LiveDataFactory
import com.eaglesakura.armyknife.android.junit4.extensions.activeAllLiveDataForTest
import com.eaglesakura.armyknife.android.junit4.extensions.compatibleBlockingTest
import com.eaglesakura.armyknife.android.junit4.extensions.instrumentationBlockingTest
import com.eaglesakura.armyknife.android.junit4.extensions.makeActivity
import com.eaglesakura.armyknife.android.junit4.extensions.makeActivityViewModel
import com.eaglesakura.armyknife.android.junit4.extensions.makeFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewModelSessionTest {

    @Test
    fun close_onDestroy() = instrumentationBlockingTest(Dispatchers.Main) {
        val activity = makeActivity(AppCompatActivity::class)
        val session = ViewModelSession<Activity>()
        session.refresh(activity)
        assertNotNull(session.value)
        assertEquals(activity, session.owner.value)
        assertEquals(activity, session.context.value)
        assertEquals(activity, session.lifecycleOwner.value)
        assertTrue(session.coroutineScope.value!!.isActive)

        activity.finish()
        delay(100)

        Log.d("ViewModelSession", "check target = $session")
        assertNull(session.value)
        assertNull(session.owner.value)
        assertNull(session.context.value)
        assertNull(session.lifecycleOwner.value)
        assertNull(session.coroutineScope.value)
        assertNull(session.value)
    }

    @Test
    fun refresh_Activity() = compatibleBlockingTest(Dispatchers.Main) {
        val activity = makeActivity(AppCompatActivity::class)
        val session = ViewModelSession<Activity>()
        session.refresh(activity)

        assertNotNull(session.value)
        assertEquals(activity, session.owner.value)
        assertEquals(activity, session.context.value)
        assertEquals(activity, session.lifecycleOwner.value)
        assertTrue(session.coroutineScope.value!!.isActive)
    }

    @Test
    fun refresh_Fragment() = compatibleBlockingTest(Dispatchers.Main) {
        val fragment = makeFragment(Fragment::class)

        val session = ViewModelSession<Fragment>()
        session.refresh(fragment)

        assertEquals(fragment, session.owner.value)
        assertEquals(fragment.requireContext(), session.context.value)
        assertEquals(fragment, session.lifecycleOwner.value)
        assertTrue(session.coroutineScope.value!!.isActive)
    }

    @Test
    fun clear() = compatibleBlockingTest(Dispatchers.Main) {
        val activity = makeActivity(AppCompatActivity::class)
        val session = ViewModelSession<Activity>()
        session.refresh(activity)
        val scope = session.coroutineScope.value!!

        session.clear()

        assertNull(session.value)
        assertNull(session.owner.value)
        assertNull(session.context.value)
        assertNull(session.lifecycleOwner.value)
        assertNull(session.coroutineScope.value)
        assertNull(session.value)
        assertFalse(scope.isActive)
    }

    @Test
    fun activityInit() = compatibleBlockingTest(Dispatchers.Main) {
        val viewModel = makeActivityViewModel { activity ->
            ViewModelProviders
                .of(activity)
                .get(ExampleActivityViewModel::class.java)
                .also { it.session.refresh(activity) }
        }

        viewModel.activeAllLiveDataForTest()
        assertTrue(viewModel.session.owner.value is Activity)
        assertEquals("OK", viewModel.message.value)
    }
}

class ExampleActivityViewModel : ViewModel() {
    val session = ViewModelSession<Activity>()

    val message = LiveDataFactory.transform(session, session.context) { token, context ->
        return@transform if (token != null && context != null) {
            "OK"
        } else {
            "Error"
        }
    }
}
