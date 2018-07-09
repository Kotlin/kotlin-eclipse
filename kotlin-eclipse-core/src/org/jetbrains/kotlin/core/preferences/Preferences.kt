package org.jetbrains.kotlin.core.preferences

import org.eclipse.core.runtime.preferences.*
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private val SCOPES = listOf(InstanceScope.INSTANCE, ConfigurationScope.INSTANCE, DefaultScope.INSTANCE)

/**
 * Wrapper of a preference node ([IEclipsePreferences]).
 * Allows querying a non-existent node without creating it.
 */
internal open class ReadableStore(protected val scope: IScopeContext, protected val path: String) {
    protected var node: IEclipsePreferences? = null

    init {
        if (scope.getNode("").nodeExists(path)) {
            loadNode()
        }
    }

    val exists: Boolean
        get() = node != null

    val keysOfValues: Set<String>
        get() = node?.keys()?.toSet() ?: emptySet()

    val keysOfChildren: Set<String>
        get() = node?.childrenNames()?.toSet() ?: emptySet()

    fun getValue(key: String): String? = node?.get(key, null)

    protected fun loadNode() {
        node = node ?: scope.getNode(path)
    }
}

/**
 * Extends [ReadableStore] with [setValue] and [deleteSelf] operations.
 * These operations modify the node.
 */
internal class WritableStore(scope: IScopeContext, path: String) : ReadableStore(scope, path) {
    fun setValue(key: String, value: String?) {
        loadNode() // Create node if it doesn't exist

        with(node!!) {
            if (value != null) put(key, value) else remove(key)
            flush()
        }
    }

    fun deleteSelf() {
        loadNode() // Create node if it doesn't exist

        val parent = node!!.parent()
        node!!.removeNode()
        parent.flush()

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

    private var exists: Boolean = mainStore.exists
    private val changes: MutableMap<String, String?> = hashMapOf()

    val existsInOwnScope: Boolean
        get() = exists

    val keysInOwnScope: Set<String>
        get() = mainStore.keysOfValues + changes.keys - changes.filterValues { it == null }.keys

    val keysInParentScopes: Set<String>
        get() = inheritedStores.flatMap { it.keysOfValues }.toSet()

    val keys: Set<String>
        get() = keysInOwnScope + keysInParentScopes

    // Does not save changes
    fun getValue(key: String): String? =
            (if (key in changes) changes[key] else mainStore.getValue(key))
                    ?: inheritedStores.firstNotNullResult { it.getValue(key) }

    // Does not save changes
    fun setValue(key: String, value: String?) {
        changes[key] = value
        exists = true
    }

    // Does not save changes
    override fun loadDefaults() {
        changes.clear()
        mainStore.keysOfValues.forEach { changes[it] = null }
        childPreferences.forEach { it.loadDefaults() }
    }

    // Does not save changes
    fun delete() {
        loadDefaults()
        exists = false
    }

    override fun saveChanges() {
        if (exists) {
            changes.forEach { key, value -> mainStore.setValue(key, value) }
            childPreferences.forEach { it.saveChanges() }
        } else {
            mainStore.deleteSelf()
        }
        changes.clear()
    }

    override fun cancelChanges() {
        changes.clear()
        exists = mainStore.exists
        childPreferences.forEach { it.cancelChanges() }
    }

    protected interface Preference<T> : ReadWriteProperty<Preferences, T> {
        fun reader(text: String?): T
        fun writer(value: T): String?

        override operator fun getValue(thisRef: Preferences, property: KProperty<*>): T =
                thisRef.getValue(property.name).let(::reader)

        override operator fun setValue(thisRef: Preferences, property: KProperty<*>, value: T) =
                thisRef.setValue(property.name, writer(value))
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
