package org.jetbrains.kotlin.core.preferences

import org.eclipse.core.runtime.preferences.IScopeContext
import org.osgi.service.prefs.Preferences as InternalPreferences
import org.eclipse.core.runtime.preferences.DefaultScope
import kotlin.reflect.KProperty
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty

abstract class Preferences protected constructor(protected val internal: InternalPreferences) {
    constructor(node: String, scope: IScopeContext = DefaultScope.INSTANCE) : this(scope.getNode(node))

    fun flush() = internal.flush()

    fun remove() = internal.removeNode()

    val key: String = internal.name()

    protected class StringPreference : ReadWriteProperty<Preferences, String?> {
        override operator fun getValue(thisRef: Preferences, property: KProperty<*>): String? =
                thisRef.internal.get(property.name, null)

        override operator fun setValue(thisRef: Preferences, property: KProperty<*>, value: String?) {
            with(thisRef.internal) {
                if (value is String) put(property.name, value)
                else remove(property.name)
            }
        }
    }

    protected class BooleanPreference : ReadWriteProperty<Preferences, Boolean> {
        override operator fun getValue(thisRef: Preferences, property: KProperty<*>): Boolean =
                thisRef.internal.getBoolean(property.name, false)

        override operator fun setValue(thisRef: Preferences, property: KProperty<*>, value: Boolean) {
            thisRef.internal.putBoolean(property.name, value)
        }
    }

    protected class ListPreference(val separator: String = "|") : ReadWriteProperty<Preferences, List<String>> {
        override fun getValue(thisRef: Preferences, property: KProperty<*>): List<String> =
                thisRef.internal.get(property.name, "").split(separator).filter(String::isNotEmpty)

        override fun setValue(thisRef: Preferences, property: KProperty<*>, value: List<String>) {
            value.joinToString(separator).also { thisRef.internal.put(property.name, it) }
        }

    }

    protected inline fun <reified T : Enum<T>> EnumPreference() =
            object : ReadWriteProperty<Preferences, T?> {
                override operator fun getValue(thisRef: Preferences, property: KProperty<*>): T? =
                        thisRef.internal.get(property.name, null)?.let { enumValueOf<T>(it) }

                override operator fun setValue(thisRef: Preferences, property: KProperty<*>, value: T?) =
                        with(thisRef.internal) {
                            if (value is Enum<*>) put(property.name, value.name)
                            else remove(property.name)
                        }
            }

    protected class Child<out T : Preferences>(val factory: (InternalPreferences) -> T) {
        operator fun provideDelegate(thisRef: Preferences, property: KProperty<*>) =
                object : ReadOnlyProperty<Preferences, T> {
                    val instance: T by lazy { thisRef.internal.node(property.name).let(factory) }

                    override fun getValue(thisRef: Preferences, property: KProperty<*>): T = instance
                }
    }

    protected class ChildCollection<T : Preferences>(val factory: (InternalPreferences) -> T) {
        operator fun provideDelegate(thisRef: Preferences, property: KProperty<*>) =
                object : ReadOnlyProperty<Preferences, PreferencesCollection<T>> {
                    val instance: PreferencesCollection<T> by lazy { PreferencesCollection(thisRef.internal.node(property.name), factory) }

                    override fun getValue(thisRef: Preferences, property: KProperty<*>): PreferencesCollection<T> = instance
                }
    }

    fun inspect(): String {
        val childInspection = internal.inspect().let { "${internal.name()}\n$it" }

        return generateSequence(internal) { it.parent() }
                .drop(1)
                .map { it.name() }
                .fold(childInspection) { acc, it ->
                    acc.lines().joinToString(prefix = "$it\n ┗━ ", separator = "\n   ")
                }
    }

    private fun InternalPreferences.inspect(): String {
        val children = (childrenNames().asSequence().map { it to this.node(it) } + keys().asSequence().map { it to this.get(it, null) }).toList()
        return children.mapIndexed { idx, (name, obj) ->
            val prefix = if (idx == children.lastIndex) " ┗━ " else " ┣━ "

            when (obj) {
                is String -> "$prefix $name = $obj"
                is InternalPreferences -> "$prefix $name" + obj.inspect().lines().joinToString(separator = "") { "\n ┃  $it" }
                else -> "$prefix $name?"
            }
        }.joinToString(separator = "\n")
    }
}

class PreferencesCollection<T>(private val internal: InternalPreferences, private val elementFactory: (InternalPreferences) -> T) {
    var cache = mutableMapOf<String, T>()

    val entries: List<T>
        get() = internal.childrenNames().toList().map(::get)

    operator fun contains(key: String) = internal.nodeExists(key)

    operator fun get(key: String): T = cache[key]
            ?: internal.node(key)
                    .let(elementFactory)
                    .also { cache[key] = it }
}