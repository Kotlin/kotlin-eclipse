package org.jetbrains.kotlin.core.preferences

import org.eclipse.core.runtime.preferences.IScopeContext
import org.osgi.service.prefs.Preferences as InternalPreferences
import org.eclipse.core.runtime.preferences.DefaultScope
import kotlin.reflect.KProperty
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty

abstract class Preferences(node: String, scope: IScopeContext = DefaultScope.INSTANCE) {
	protected val internal: InternalPreferences = scope.getNode(node)

	public fun flush() = internal.flush()

	protected class Preference {
		operator fun getValue(thisRef: Preferences, property: KProperty<*>): String? =
				thisRef.internal.get(property.name, null)

		operator fun setValue(thisRef: Preferences, property: KProperty<*>, newValue: String?) {
			with(thisRef.internal) {
				if (newValue is String) put(property.name, newValue)
				else remove(property.name)
			}
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

			if (obj is String) "$prefix $name = $obj"
			else if (obj is InternalPreferences) "$prefix $name" + obj.inspect().lines().map { "\n ┃  $it" }.joinToString(separator = "")
			else "$prefix $name?"
		}.joinToString(separator = "\n")
	}
}