package org.jetbrains.kotlin.utils

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class LazyObservable<T>(
        initialValueProvider: () -> T,
        private val onChange: (KProperty<*>, T, T) -> Unit
) : ReadWriteProperty<Any?, T> {
    private var overridden: Boolean = false

    private var container: Any? = lazy(LazyThreadSafetyMode.NONE, initialValueProvider)

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any?, property: KProperty<*>) =
            if (!overridden)
                (container as Lazy<T>).getValue(thisRef, property)
            else
                container as T

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val oldValue = getValue(thisRef, property)
        overridden = true
        container = value
        onChange(property, oldValue, value)
    }

}