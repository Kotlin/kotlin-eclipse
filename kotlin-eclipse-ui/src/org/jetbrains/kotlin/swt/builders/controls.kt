package org.jetbrains.kotlin.swt.builders

import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Combo
import kotlin.properties.ReadWriteProperty
import org.jetbrains.kotlin.core.preferences.Preferences
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import kotlin.reflect.KMutableProperty0
import org.jetbrains.kotlin.core.log.KotlinLogger

const val DEFAULT = "Default"

inline fun Composite.label(text: String = "", style: Int = SWT.NONE, operations: Label.() -> Unit = {}) =
		Label(this, style).apply {
			this.text = text
			operations()
		}

inline fun <reified T : Enum<T>> Composite.enumPreference(
		delegate: KMutableProperty0<T?>,
		nameProvider: (T) -> String = { it.toString() },
		style: Int = SWT.NONE,
		operations: Combo.() -> Unit = {}
) =
		Combo(this, style or SWT.READ_ONLY).apply {
			val valuesMapping = enumValues<T>()
					.associateByTo(LinkedHashMap<String, T?>(), nameProvider)
					.apply { put(DEFAULT, null) }
			valuesMapping.keys.forEach { add(it) }
			
			val selected = delegate.get()
					?.let(nameProvider)
					?.let(valuesMapping.keys::indexOf)
					?: (valuesMapping.size - 1)
			select(selected)
			
			
			addSelectionListener(object : SelectionAdapter() {
				override fun widgetSelected(event: SelectionEvent) {
					delegate.set(valuesMapping[(event.widget as Combo).text])
				}
			})
			
			operations()
		}

inline fun Composite.gridContainer(cols: Int = 1, style: Int = SWT.NONE, operations: Composite.() -> Unit = {}) =
		Composite(this, style).apply {
			layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
			layout = GridLayout(cols, false)
			operations()
		}
