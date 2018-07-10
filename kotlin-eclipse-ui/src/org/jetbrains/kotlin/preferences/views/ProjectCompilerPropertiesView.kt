package org.jetbrains.kotlin.preferences.views

import org.eclipse.swt.widgets.Composite
import org.jetbrains.kotlin.core.preferences.KotlinProperties
import org.jetbrains.kotlin.swt.builders.*
import kotlin.properties.Delegates

fun View<Composite>.projectCompilerPropertiesView(
        kotlinProperties: KotlinProperties,
        operations: ProjectCompilerPropertiesView.() -> Unit = {}
) =
        ProjectCompilerPropertiesView(this, kotlinProperties)
                .apply(operations)

class ProjectCompilerPropertiesView(
        parent: View<Composite>,
        private val kotlinProperties: KotlinProperties
) : View<Composite>, Validable {

    override val control: Composite

    override val isValid get() = innerView.isValid

    override var onIsValidChanged
        get() = innerView.onIsValidChanged
        set(value) {
            innerView.onIsValidChanged = value
        }

    private lateinit var innerView: CompilerPropertiesView

    private var overrideFlag by Delegates.observable(kotlinProperties.globalsOverridden) { _, _, value ->
        kotlinProperties.globalsOverridden = value
        innerView.enabled = value
    }

    init {
        control = parent.gridContainer {
            checkbox(::overrideFlag, "Enable project specific settings")
            separator()
            innerView = compilerPropertiesView(kotlinProperties) {
                enabled = kotlinProperties.globalsOverridden
                layout(horizontalGrab = true, verticalGrab = true)
            }
        }.control
    }
}