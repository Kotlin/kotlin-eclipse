package org.jetbrains.kotlin.preferences.views

import org.eclipse.swt.widgets.Composite
import org.jetbrains.kotlin.core.preferences.KotlinBuildingProperties
import org.jetbrains.kotlin.swt.builders.View
import org.jetbrains.kotlin.swt.builders.checkbox
import org.jetbrains.kotlin.swt.builders.gridContainer
import kotlin.properties.Delegates

fun View<Composite>.buildingPropertiesView(
    kotlinBuildingProperties: KotlinBuildingProperties,
    operations: BuildingPropertiesView.() -> Unit = {}
) =
    BuildingPropertiesView(this, kotlinBuildingProperties)
        .apply(operations)

class BuildingPropertiesView(
    parent: View<Composite>,
    kotlinBuildingProperties: KotlinBuildingProperties
) : View<Composite>, Validable {

    override val control: Composite

    override var isValid: Boolean = true
        private set(value) {
            field = value
            onIsValidChanged(value)
        }

    override var onIsValidChanged: (Boolean) -> Unit = {}

    private var useIncremental by Delegates.observable(kotlinBuildingProperties.useIncremental) { _, _, value ->
        kotlinBuildingProperties.useIncremental = value
    }

    private var alwaysRealBuild by Delegates.observable(kotlinBuildingProperties.alwaysRealBuild) { _, _, value ->
        kotlinBuildingProperties.alwaysRealBuild = value
    }

    init {
        control = parent.gridContainer {
            checkbox(::useIncremental, "Use incremental compiler (experimental)")
            checkbox(::alwaysRealBuild, "Always build real class files")
        }.control
    }
}
