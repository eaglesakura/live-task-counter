package com.eaglesakura.firearm.viewmodel

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders

interface ViewModelOwner {
    fun <T : ViewModel> getViewModel(clazz: Class<T>): T

    val context: Context

    val lifecycle: Lifecycle
        get() = lifecycleOwner.lifecycle

    val lifecycleOwner: LifecycleOwner
        get() = (owner as LifecycleOwner)

    val owner: Any

    companion object {
        fun from(activity: FragmentActivity): ViewModelOwner {
            return object : ViewModelOwner {
                override val context: Context
                    get() = activity

                override val lifecycle: Lifecycle
                    get() = activity.lifecycle

                override val lifecycleOwner: LifecycleOwner
                    get() = activity

                override val owner: Any
                    get() = activity

                override fun <T : ViewModel> getViewModel(clazz: Class<T>): T {
                    return ViewModelProviders.of(activity).get(clazz)
                }
            }
        }

        fun from(fragment: Fragment): ViewModelOwner {
            return object : ViewModelOwner {
                override val context: Context
                    get() = fragment.context!!
                override val lifecycle: Lifecycle
                    get() = fragment.lifecycle
                override val lifecycleOwner: LifecycleOwner
                    get() = fragment
                override val owner: Any
                    get() = fragment

                override fun <T : ViewModel> getViewModel(clazz: Class<T>): T {
                    return ViewModelProviders.of(fragment).get(clazz)
                }
            }
        }
    }
}
