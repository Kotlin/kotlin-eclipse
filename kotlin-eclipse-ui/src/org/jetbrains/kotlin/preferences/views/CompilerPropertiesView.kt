package org.jetbrains.kotlin.preferences.views

import javafx.beans.property.StringProperty
import org.eclipse.jface.resource.FontDescriptor
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.core.preferences.CompilerPlugin
import org.jetbrains.kotlin.core.preferences.KotlinProperties
import org.jetbrains.kotlin.preferences.compiler.CompilerPluginDialog
import org.jetbrains.kotlin.swt.builders.*
import org.jetbrains.kotlin.utils.LazyObservable
import java.awt.TextField
import kotlin.properties.Delegates

fun View<Composite>.compilerPropertiesView(
        kotlinProperties: KotlinProperties,
        operations: CompilerPropertiesView.() -> Unit = {}
) =
        CompilerPropertiesView(this, kotlinProperties)
                .apply(operations)

class CompilerPropertiesView(
        parent: View<Composite>,
        private val kotlinProperties: KotlinProperties
) : View<Composite>, Validable {

    override val control: Composite

    override var isValid: Boolean = true
        private set(value) {
            field = value
            onIsValidChanged(value)
        }

    override var onIsValidChanged: (Boolean) -> Unit = {}

    private var languageVersionProxy: LanguageVersion by LazyObservable(
            initialValueProvider = { kotlinProperties.languageVersion },
            onChange = { _, _, value ->
                kotlinProperties.languageVersion = value
                checkApiVersionCorrectness()
            }
    )

    private var apiVersionProxy: ApiVersion by LazyObservable(
            initialValueProvider = { kotlinProperties.apiVersion },
            onChange = { _, _, value ->
                kotlinProperties.apiVersion = value
                checkApiVersionCorrectness()
            }
    )

    private var jdkHomeProxy: String? by LazyObservable(
            initialValueProvider = { kotlinProperties.jdkHome },
            onChange = { _, oldValue, value ->
                if (oldValue != value) {
                    kotlinProperties.jdkHome = value
                    jdkHomeTextField.update(value.orEmpty())
                }
            }
    )

    private lateinit var apiVersionErrorLabel: Label

    private lateinit var jdkHomeTextField: View<Text>

    private var selectedPlugin by Delegates.observable<CompilerPlugin?>(null) { _, _, value ->
        val source = value?.source

        editButton.enabled = source != null
        removeButton.enabled = source != null && source != CompilerPluginSource.Inherited
        removeButton.label =
                if (source == CompilerPluginSource.InheritedOverridden) "Restore"
                else "Remove"
    }

    private lateinit var editButton: View<Button>

    private lateinit var removeButton: View<Button>

    init {
        control = parent.gridContainer(cols = 2) {
            label("JVM target version: ")
            singleOptionPreference(kotlinProperties::jvmTarget,
                    allowedValues = enumValues<JvmTarget>().asList(),
                    nameProvider = JvmTarget::description) {
                layout(horizontalGrab = true)
            }
            label("Language version: ")
            singleOptionPreference(::languageVersionProxy,
                    allowedValues = enumValues<LanguageVersion>().asList(),
                    nameProvider = LanguageVersion::description) {
                layout(horizontalGrab = true)
            }
            label("API version: ")
            singleOptionPreference(::apiVersionProxy,
                    allowedValues = enumValues<LanguageVersion>().map { ApiVersion.createByLanguageVersion(it) },
                    nameProvider = ApiVersion::description) {
                layout(horizontalGrab = true)
            }
            label("JDK Home: ")
            gridContainer(cols = 2) {
                with(control.layout as GridLayout) {
                    marginWidth = 0
                    marginHeight = 0
                }
                jdkHomeTextField = textField(::jdkHomeProxy, style = SWT.SINGLE or SWT.BORDER) {
                    layout(
                        horizontalGrab = true, verticalAlignment = SWT.CENTER
                    )
                }
                button(label = "Browse") {
                    layout(verticalAlignment = SWT.CENTER)
                    onClick {
                        DirectoryDialog(control.shell).open()?.let { jdkHomeProxy = it }
                    }
                }
            }
            label("")
            apiVersionErrorLabel = label("API version must be lower or equal to language version")
                    .control
                    .apply {
                        font = FontDescriptor.createFrom(control.font)
                                .increaseHeight(-1)
                                .createFont(Display.getCurrent())
                        foreground = Display.getCurrent().getSystemColor(SWT.COLOR_RED)
                        visible = false
                    }
            group("Compiler plugins:", cols = 2) {
                layout(horizontalSpan = 2, verticalGrab = true)
                val list = checkList({ kotlinProperties.compilerPlugins.entries.sortedBy { it.key } },
                        selectionDelegate = ::selectedPlugin,
                        style = SWT.BORDER) {
                    layout(horizontalGrab = true, verticalGrab = true, verticalSpan = 4)
                    nameProvider = { it.description }
                    checkDelegate = CompilerPlugin::active
                }
                button("Add") {
                    onClick {
                        CompilerPluginDialog(control.shell, kotlinProperties.compilerPlugins, null).open()
                        list.refresh()
                    }
                }
                editButton = button("Edit") {
                    enabled = false
                    onClick {
                        selectedPlugin?.also { CompilerPluginDialog(control.shell, kotlinProperties.compilerPlugins, it).open() }
                        list.refresh()
                    }
                }
                removeButton = button("Remove") {
                    enabled = false
                    onClick {
                        selectedPlugin?.delete()
                        list.refresh()
                    }
                }
            }
            group("Additional compiler flags") {
                layout(horizontalSpan = 2, verticalGrab = true)
                textField(kotlinProperties::compilerFlags, style = SWT.MULTI) { layout(horizontalGrab = true, verticalGrab = true) }
            }
        }.control
    }

    private fun checkApiVersionCorrectness() {
        val correct = apiVersionProxy <= ApiVersion.createByLanguageVersion(languageVersionProxy)
        apiVersionErrorLabel.visible = !correct
        isValid = correct
    }

    private val CompilerPlugin.source: CompilerPluginSource
        get() = when {
            this.keysInParentScopes.isEmpty() ->
                CompilerPluginSource.Own
            (this.keysInOwnScope - CompilerPlugin::active.name).isEmpty() ->
                CompilerPluginSource.Inherited
            else ->
                CompilerPluginSource.InheritedOverridden
        }

    private val CompilerPlugin.description: String
        get() =
            this.key + when (this.source) {
                CompilerPluginSource.Own -> ""
                CompilerPluginSource.Inherited -> " (inherited)"
                CompilerPluginSource.InheritedOverridden -> " (inherited, overridden)"
            }

    private enum class CompilerPluginSource {
        Own,
        Inherited,
        InheritedOverridden
    }
}

