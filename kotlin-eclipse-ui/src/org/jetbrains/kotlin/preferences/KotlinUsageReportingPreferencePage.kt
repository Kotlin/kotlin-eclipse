/*******************************************************************************
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.preferences

import org.eclipse.ui.IWorkbenchPreferencePage
import org.eclipse.jface.preference.PreferencePage
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Composite
import org.eclipse.ui.IWorkbench
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Group
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.jface.preference.IPreferenceStore
import org.jetbrains.kotlin.ui.Activator
import org.eclipse.jdt.internal.ui.preferences.OverlayPreferenceStore.BOOLEAN
import org.eclipse.jdt.internal.ui.preferences.OverlayPreferenceStore.OverlayKey
import org.jetbrains.kotlin.ui.KotlinUsageReporter
import org.eclipse.jdt.internal.ui.preferences.OverlayPreferenceStore
import org.eclipse.jface.layout.GridDataFactory
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.custom.StyleRange
import java.util.ArrayList

public class KotlinUsageReportingPreferencePage : PreferencePage(), IWorkbenchPreferencePage {
    private val overlayStore = run {
        val key = OverlayKey(BOOLEAN, KotlinUsageReporter.UPDATE_USAGE_AVAILABLE_KEY)
        OverlayPreferenceStore(getPreferenceStore(), arrayOf(key))
    }
    
    override fun createContents(parent: Composite): Control {
        val composite = Composite(parent, SWT.NONE)
        
        val layout = GridLayout(1, true)
        layout.marginHeight = 0
        layout.marginWidth = 0
        composite.setLayout(layout)
        
        createSettingsGroup(composite)
        createReportedValuesGroup(composite)
        
        return composite
    }
    
    override fun init(workbench: IWorkbench) {
    }
    
    override fun getPreferenceStore(): IPreferenceStore = Activator.getDefault().preferenceStore
    
    override fun performDefaults() {
        super.performDefaults()
        overlayStore.loadDefaults()
    }
    
    override fun performOk(): Boolean {
        super.performOk()
        overlayStore.propagate()
        
        return true
    }
    
    override fun performCancel(): Boolean {
        overlayStore.stop()
        return super.performCancel()
    }
    
    override fun dispose() {
        overlayStore.stop()
        super.dispose()
    }
    
    private fun createSettingsGroup(composite: Composite) {
        overlayStore.load()
        overlayStore.start()
        
        val group = Group(composite, SWT.NONE)
        group.setText("Usage Reporting")
        group.setLayout(GridLayout(1, false))
        group.setLayoutData(gridData(verticalAlignment = SWT.TOP))
        
        with(Button(group, SWT.CHECK)) {
            setText("Allow the Kotlin plugin team to receive anonymous usage statistics for this Eclipse installation with Kotlin Plugin")
            setLayoutData(gridData())
            
            setSelection(overlayStore.getBoolean(KotlinUsageReporter.UPDATE_USAGE_AVAILABLE_KEY))
            
            addWidgetSelectionListener { 
                overlayStore.setValue(KotlinUsageReporter.UPDATE_USAGE_AVAILABLE_KEY, getSelection())
            }
        }
    }
    
    private fun createReportedValuesGroup(composite: Composite) {
        val group = Group(composite, SWT.NONE)
        group.setText("Reported values")
        
        GridDataFactory.fillDefaults().grab(true, true).hint(SWT.FILL, SWT.FILL).applyTo(group);
        val fillLayout = FillLayout()
        fillLayout.marginHeight = 4;
        fillLayout.marginWidth = 8;
        group.setLayout(fillLayout);
        
        val text = StyledText(group, SWT.BORDER or SWT.V_SCROLL or SWT.H_SCROLL)
        text.setEditable(false)
        
        val styleRanges = arrayListOf<StyleRange>()
        
        val newLine = System.lineSeparator()
        val builder = StringBuilder()
        addBoldText(builder, "Kotlin plugin version: ", styleRanges)
        builder.append(KotlinUsageReporter.getKotlinPluginVersion())
        builder.append(newLine)
        
        addBoldText(builder, "Eclipse platform version: ", styleRanges)
        builder.append(KotlinUsageReporter.getPlatformVersion())
        builder.append(newLine)
        
        addBoldText(builder, "Operating system: ", styleRanges)
        builder.append(KotlinUsageReporter.getOperatingSystem())
        
        text.setText(builder.toString())
        
        text.setStyleRanges(styleRanges.toTypedArray())
    }
    
    private fun addBoldText(builder: StringBuilder, text: String, ranges: ArrayList<StyleRange>) {
        ranges.add(StyleRange().apply {
            start = builder.length
            length = text.length
            fontStyle = SWT.BOLD
        })
        builder.append(text)
    }
    
    private fun Button.addWidgetSelectionListener(action: (SelectionEvent?) -> Unit) {
        addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent?) {
                action(e)
            }
        })
    }
    
    private fun gridData(
            horizontalAlignment: Int = SWT.BEGINNING,
            verticalAlignment: Int = SWT.CENTER,
            grabExcessHorizontalSpace: Boolean = false,
            grabExcessVerticalSpace: Boolean = false,
            horizontalSpan: Int = 1,
            verticalSpan: Int = 1): GridData {
        return GridData(horizontalAlignment, verticalAlignment, grabExcessHorizontalSpace, grabExcessVerticalSpace, horizontalSpan, verticalSpan)
    }
}