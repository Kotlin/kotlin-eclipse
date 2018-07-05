package org.jetbrains.kotlin.preferences

import org.eclipse.jface.dialogs.IDialogConstants
import org.eclipse.jface.dialogs.MessageDialog
import org.eclipse.jface.dialogs.TrayDialog
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text
import org.jetbrains.kotlin.core.preferences.CompilerPlugin
import org.jetbrains.kotlin.core.preferences.PreferencesCollection
import org.jetbrains.kotlin.swt.builders.*

class CompilerPluginDialog(
        shell: Shell,
        private val allPlugins: PreferencesCollection<CompilerPlugin>,
        private val plugin: CompilerPlugin?
) : TrayDialog(shell) {
    private var keyField = plugin?.key

    private var pathField = plugin?.jarPath
    
    private var argsField = plugin?.args?.joinToString(separator = "\n")
    
    private val newKey: String
        get() = keyField?.trim() ?: ""


    override fun createDialogArea(parent: Composite): Control =
            parent.asView.gridContainer(cols = 2) {
                (control.layout as GridLayout).marginTop = 10
                label("Plugin name:")
                textField(::keyField, style = SWT.BORDER) {
                    layout(horizontalGrab = true)
                }
                label("Path to jar:")
                textField(::pathField, style = SWT.BORDER) {
                    layout(horizontalGrab = true)
                }
                group("Plugin options:") {
                    layout(horizontalSpan = 2, horizontalGrab = true, verticalGrab = true)
                    textField(::argsField, style = SWT.BORDER or SWT.MULTI or SWT.V_SCROLL) {
                        layout(suggestedHeight = 200, suggestedWidth = 450)
                    }
                    label("Note: plugin options should match the <plugin id>:<key>=<value> pattern")
                }
            }.control

    override fun configureShell(newShell: Shell) {
        super.configureShell(newShell)
        newShell.text = if (plugin == null) "New compiler plugin" else "Edit compiler plugin"
    }

    override fun isHelpAvailable() = false

    override fun buttonPressed(buttonId: Int) {
        val successful = try {
            when {
                buttonId != IDialogConstants.OK_ID -> true

                keyField.isNullOrBlank() -> throw ValidationException("Plugin name cannot be blank")

                plugin != null && newKey == plugin.key -> {
                    with(plugin) {
                        jarPath = pathField?.trim()
                        args = processArgs()
                    }
                    true
                }

                newKey in allPlugins -> throw ValidationException("Plugin with chosen name already exists")

                else -> {
                    plugin?.delete()
                    with(allPlugins[newKey]) {
                        jarPath = pathField?.trim()
                        active = plugin?.active ?: true
                        args = processArgs()
                    }
                    true
                }
            }
        } catch (e: ValidationException) {
            MessageDialog.openError(shell, "Validation error", e.message)
            false
        }


        if (successful) {
            super.buttonPressed(buttonId)
        }
    }
    
    private fun processArgs() : List<String> {
        val processedArgs = argsField.orEmpty()
                .lineSequence()
                .map(String::trim)
                .filterNot(String::isEmpty)
                .toList()

        processedArgs.find { """.+:.+=.+""".toRegex().matchEntire(it) == null }?.also {
            throw ValidationException("$it does not match the <plugin id>:<key>=<value> pattern.")
        }

        val pluginPrefixes = processedArgs.map { """^.+:""".toRegex().find(it)?.value }
                .toSet()
        if (pluginPrefixes.size > 1) {
            throw ValidationException("Plugin arguments have different prefixes")
        }

        return processedArgs
    }

    override fun createButtonsForButtonBar(parent: Composite) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true)
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false)
    }
}

private class ValidationException(message: String): Exception(message)