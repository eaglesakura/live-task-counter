package com.eaglesakura.firearm.extensions

import androidx.fragment.app.Fragment
import com.eaglesakura.armyknife.android.extensions.findInterface
import com.eaglesakura.armyknife.runtime.extensions.instanceOf
import com.eaglesakura.firearm.viewmodel.ViewModelOwner
import kotlin.reflect.KClass

/**
 * Find interface from ViewModelOwner, and cast to "T".
 */
fun <T : Any> ViewModelOwner?.findInterface(clazz: KClass<T>): T? {
    val owner = this?.owner ?: false
    return when {
        owner.instanceOf(clazz) -> {
            @Suppress("UNCHECKED_CAST")
            owner as? T
        }
        owner is Fragment -> {
            owner.findInterface(clazz)
        }
        else -> null
    }
}