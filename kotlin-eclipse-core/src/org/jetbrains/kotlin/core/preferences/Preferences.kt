package org.jetbrains.kotlin.core.preferences

import org.eclipse.core.internal.preferences.PreferencesService
import org.eclipse.core.runtime.preferences.ConfigurationScope
import org.eclipse.core.runtime.preferences.DefaultScope
import org.eclipse.core.runtime.preferences.IScopeContext
import org.eclipse.core.runtime.preferences.InstanceScope
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import org.osgi.service.prefs.Preferences as Store

private val SCOPES = listOf(InstanceScope.INSTANCE, ConfigurationScope.INSTANCE, DefaultScope.INSTANCE)

abstract class PreferencesBase(scope: IScopeContext, path: String) {
    protected val readStores = SCOPES.takeLastWhile { scope::class != it::class }
            .filter { PreferencesService.getDefault().rootNode.node(it.name).nodeExists(path) }
            .let { listOf(scope) + it }
            .map { it.getNode(path) }
            .asSequence()

    protected val writeStore: Store = readStores.first()
}

abstract class Preferences(private val scope: IScopeContext, private val path: String) : PreferencesBase(scope, path) {
    fun flush() = writeStore.flush()

    fun remove() = writeStore.removeNode()

    val key: String = writeStore.name()

    private fun getValue(key: String): String? =
            readStores.map { it.get(key, null) }
                    .firstOrNull { it != null }


    protected interface Preference<T> : ReadWriteProperty<Preferences, T> {
        fun reader(text: String?): T
        fun writer(value: T): String?

        override operator fun getValue(thisRef: Preferences, property: KProperty<*>): T =
                thisRef.getValue(property.name).let(::reader)

        override operator fun setValue(thisRef: Preferences, property: KProperty<*>, value: T) {
            val text = writer(value)
            with(thisRef.writeStore) {
                if (text != null) put(property.name, text)
                else remove(property.name)
            }
        }
    }

    protected class StringPreference : Preference<String?> {
        override fun reader(text: String?) = text
        override fun writer(value: String?) = value
    }

    protected class BooleanPreference : Preference<Boolean> {
        override fun reader(text: String?) = text?.toBoolean() ?: false
        override fun writer(value: Boolean) = value.toString()
    }

    protected class ListPreference(val separator: String = "|") : Preference<List<String>> {
        override fun reader(text: String?) = text?.split(separator)?.filter(String::isNotEmpty) ?: listOf()
        override fun writer(value: List<String>) = value.joinToString(separator)
    }

    protected inline fun <reified T : Enum<T>> EnumPreference(defaultValue: T): ReadWriteProperty<Preferences, T> =
            object : Preference<T> {
                override fun reader(text: String?) = text?.let { enumValueOf<T>(it) } ?: defaultValue
                override fun writer(value: T) = value.name
            }

    protected class Child<out T : Preferences>(val factory: (IScopeContext, String) -> T) {
        operator fun provideDelegate(thisRef: Preferences, property: KProperty<*>) =
                object : ReadOnlyProperty<Preferences, T> {
                    val instance: T by lazy { factory(thisRef.scope, "${thisRef.path}/${property.name}") }

                    override fun getValue(thisRef: Preferences, property: KProperty<*>): T = instance
                }
    }

    protected class ChildCollection<T: Preferences>(val factory: (IScopeContext, String) -> T) {
        operator fun provideDelegate(thisRef: Preferences, property: KProperty<*>) =
                object : ReadOnlyProperty<Preferences, PreferencesCollection<T>> {
                    val instance by lazy { PreferencesCollection(thisRef.scope, "${thisRef.path}/${property.name}", factory) }

                    override fun getValue(thisRef: Preferences, property: KProperty<*>): PreferencesCollection<T> = instance
                }
    }
}

class PreferencesCollection<T>(
        private val scope: IScopeContext,
        private val path: String,
        private val elementFactory: (IScopeContext, String) -> T
) : PreferencesBase(scope, path) {

    var cache = mutableMapOf<String, T>()

    val entries: List<T>
        get() = readStores
                .flatMap { it.childrenNames().asSequence() }
                .distinct()
                .map { get(it) }
                .toList()

    operator fun contains(key: String) = readStores.any { it.nodeExists(key) }

    operator fun get(key: String): T = cache[key]
            ?: elementFactory(scope, "$path/$key")
                    .also { cache[key] = it }
}