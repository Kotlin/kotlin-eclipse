package org.jetbrains.kotlin.swt.builders

import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Table
import org.eclipse.swt.widgets.TableItem
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent

class ChecklistView<T>(control: Table, val model: () -> Iterable<T>) : View<Table>(control) {
    var nameProvider: (T) -> String = { it.toString() }

    var checkDelegate: KMutableProperty1<T, Boolean>? = null

    var selected: T? = null
        private set

    @PublishedApi
    internal fun setSelection(element: T) {
        selected = element
    }
    
    fun refresh() {
        control.removeAll()
        model().forEach {
                TableItem(control, SWT.NONE).apply {
                    checked = checkDelegate?.invoke(it) ?: false
                    text = nameProvider(it)
                    data = it
                }
            }
    }
}

inline fun <reified T> View<Composite>.checkList(
        noinline model: () -> Iterable<T>,
        style: Int = SWT.NONE,
        operations: ChecklistView<T>.() -> Unit = {}
) =
        ChecklistView<T>(Table(control, style or SWT.CHECK or SWT.SINGLE), model).apply {
            operations()
            refresh()
            control.addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent): Unit = with(e.item as TableItem) {
                    if (e.detail and SWT.CHECK != 0) {
                        checkDelegate?.set(this.data as T, checked)
                    } else {
                        setSelection(this.data as T)
                    }
                }
            })
        }.applyDefaultLayoutIfNeeded()