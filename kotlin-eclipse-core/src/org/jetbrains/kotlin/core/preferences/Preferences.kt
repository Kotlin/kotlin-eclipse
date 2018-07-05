package org.jetbrains.kotlin.core.preferences

import org.eclipse.core.runtime.preferences.ConfigurationScope
import org.eclipse.core.runtime.preferences.DefaultScope
import org.eclipse.core.runtime.preferences.IScopeContext
import org.eclipse.core.runtime.preferences.InstanceScope
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import org.osgi.service.prefs.Preferences as OsgiPreferences

private val SCOPES = listOf(InstanceScope.INSTANCE, ConfigurationScope.INSTANCE, DefaultScope.INSTANCE)

/**
 * Wrapper of a preference node ([OsgiPreferences]).
 * Allows querying a non-existent node without creating it.
 */
internal open class ReadableStore(protected val scope: IScopeContext, protected val path: String) {
    protected var node: OsgiPreferences? =
            if (scope.getNode("").nodeExists(path)) scope.getNode(path)
            else null

    protected val guaranteedNode: OsgiPreferences
        get() = node ?: scope.getNode(path).also { node = it }

    val exists: Boolean
        get() = node != null

    val keysOfValues: Set<String>
        get() = node?.keys()?.toSet() ?: emptySet()

    val keysOfChildren: Set<String>
        get() = node?.childrenNames()?.toSet() ?: emptySet()

    fun getValue(key: String): String? = node?.get(key, null)
}

/**
 * Extends [ReadableStore] with [setValue], [removeValue] and [deleteSelf] operations.
 * These operations modify the node.
 */
internal class WritableStore(scope: IScopeContext, path: String) : ReadableStore(scope, path) {
    fun setValue(key: String, value: String) {
        // Create node if it doesn't exist
        with(guaranteedNode) {
            put(key, value)
            flush()
        }
    }

    fun removeValue(key: String) {
        // Create node if it doesn't exist
        with(guaranteedNode) {
            remove(key)
            flush()
        }
    }

    fun deleteSelf() {
        // Create and then delete node
        with(guaranteedNode) {
            val parent = parent()
            removeNode()
            parent.flush()
        }
        node = null
    }
}

abstract class PreferencesBase(scope: IScopeContext, path: String) {
    internal val mainStore = WritableStore(scope, path)
    internal val inheritedStores = SCOPES.takeLastWhile { scope::class != it::class }
            .map { ReadableStore(it, path) }

    abstract fun loadDefaults() // Does not save changes

    abstract fun saveChanges()
    abstract fun cancelChanges()
}

abstract class Preferences(private val scope: IScopeContext, private val path: String) : PreferencesBase(scope, path) {

    val key: String = path.takeLastWhile { it != '/' }

    private val childPreferences: MutableList<PreferencesBase> = mutableListOf()

    // Unsaved changes
    private var markedForDeletion: Boolean = !mainStore.exists
    private val modifiedValues: MutableMap<String, String> = hashMapOf()
    private val removedValues: MutableSet<String> = hashSetOf()

    val existsInOwnScope: Boolean
        get() = !markedForDeletion

    val keysInOwnScope: Set<String>
        get() = mainStore.keysOfValues + modifiedValues.keys - removedValues

    val keysInParentScopes: Set<String>
        get() = inheritedStores.flatMap { it.keysOfValues }.toSet()

    val keys: Set<String>
        get() = keysInOwnScope + keysInParentScopes

    // Does not save changes
    fun getValue(key: String): String? =
            when (key) {
                in modifiedValues -> modifiedValues[key]
                in removedValues -> null
                else -> mainStore.getValue(key)
            } ?: inheritedStores.firstNotNullResult { it.getValue(key) }

    // Does not save changes
    fun setValue(key: String, value: String) {
        removedValues -= key
        modifiedValues[key] = value
        markedForDeletion = false
    }

    // Does not save changes
    fun removeValue(key: String) {
        modifiedValues -= key
        removedValues += key
        markedForDeletion = false
    }

    // Does not save changes
    override fun loadDefaults() {
        modifiedValues.clear()
        removedValues.clear()

        removedValues += mainStore.keysOfValues
        markedForDeletion = false
        childPreferences.forEach { it.loadDefaults() }
    }

    // Does not save changes
    fun delete() {
        loadDefaults()
        markedForDeletion = true
    }

    override fun saveChanges() {
        if (markedForDeletion) {
            mainStore.deleteSelf()
        } else {
            modifiedValues.forEach { key, value -> mainStore.setValue(key, value) }
            removedValues.forEach { key -> mainStore.removeValue(key) }
            childPreferences.forEach { it.saveChanges() }
        }
        modifiedValues.clear()
        removedValues.clear()
    }

    override fun cancelChanges() {
        modifiedValues.clear()
        removedValues.clear()
        markedForDeletion = !mainStore.exists
        childPreferences.forEach { it.cancelChanges() }
    }

    protected interface Preference<T> : ReadWriteProperty<Preferences, T> {
        fun reader(text: String?): T
        fun writer(value: T): String?

        override operator fun getValue(thisRef: Preferences, property: KProperty<*>): T =
                thisRef.getValue(property.name).let(::reader)

        override operator fun setValue(thisRef: Preferences, property: KProperty<*>, value: T) =
                writer(value).let {
                    if (it != null) thisRef.setValue(property.name, it)
                    else thisRef.removeValue(property.name)
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

    protected class ChildCollection<T : Preferences>(val factory: (IScopeContext, String) -> T) {
        operator fun provideDelegate(thisRef: Preferences, property: KProperty<*>) =
                object : ReadOnlyProperty<Preferences, PreferencesCollection<T>> {
                    val instance by lazy {
                        PreferencesCollection(thisRef.scope, "${thisRef.path}/${property.name}", factory)
                                .also { thisRef.childPreferences.add(it) }
                    }

                    override fun getValue(thisRef: Preferences, property: KProperty<*>): PreferencesCollection<T> = instance
                }
    }
}

class PreferencesCollection<T : Preferences>(
        private val scope: IScopeContext,
        private val path: String,
        private val elementFactory: (IScopeContext, String) -> T
) : PreferencesBase(scope, path) {

    private val cache = mutableMapOf<String, T>()

    val entries: List<T>
        get() = keys.map { get(it) }

    private val keysInOwnScope: Set<String>
        get() = mainStore.keysOfChildren + cache.keys - cache.filterValues { !it.existsInOwnScope }.keys

    private val keysInParentScopes: Set<String>
        get() = inheritedStores.flatMap { it.keysOfChildren }.toSet()

    private val keys: Set<String>
        get() = keysInOwnScope + keysInParentScopes

    private fun makeCache(key: String): T = cache.getOrPut(key) { elementFactory(scope, "$path/$key") }

    operator fun contains(key: String): Boolean = key in keys

    operator fun get(key: String): T = makeCache(key)

    // Does not save changes
    override fun loadDefaults() {
        cache.clear()
        entries.forEach { it.delete() }
    }

    override fun saveChanges() {
        cache.values.forEach { it.saveChanges() }
    }

    override fun cancelChanges() {
        cache.clear()
    }

}
