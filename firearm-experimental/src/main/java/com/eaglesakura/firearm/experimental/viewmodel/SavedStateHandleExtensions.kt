package com.eaglesakura.firearm.experimental.viewmodel

import androidx.lifecycle.SavedStateHandle
import kotlin.reflect.KProperty

/**
 * make accessor to simple value.
 *
 * e.g.)
 *
 * class ExampleViewModel(val handle: SavedStateHandle): ViewModel() {
 *      val url: String by handle.delegateValue("url", "https://example.com")
 * }
 */
fun <T> SavedStateHandle.delegateValue(key: String, defValue: T) =
    HandleValueDelegate(this, key, defValue)

class HandleValueDelegate<T> internal constructor(
    private val handle: SavedStateHandle,
    private val key: String,
    private val defValue: T
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = handle.get(key) ?: defValue

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        handle.set(key, value)
    }
}