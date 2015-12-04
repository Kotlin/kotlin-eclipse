package org.jetbrains.kotlin.preferences

import org.eclipse.jface.preference.PreferencePage
import org.eclipse.ui.IWorkbenchPreferencePage
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.ui.IWorkbench
import org.eclipse.swt.SWT
import org.eclipse.jdt.internal.ui.preferences.ScrolledPageContent
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Link
import org.eclipse.jdt.internal.ui.preferences.PreferencesMessages
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.ui.dialogs.PreferencesUtil
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.Label
import org.eclipse.jface.viewers.TreeViewer
import org.jetbrains.kotlin.ui.editors.highlighting.SEMANTIC_SYNTAX_CATEGORY
import org.eclipse.swt.widgets.Group
import org.eclipse.swt.widgets.Button
import org.eclipse.jface.preference.ColorSelector
import org.eclipse.jface.viewers.IStructuredSelection
import org.jetbrains.kotlin.ui.editors.highlighting.KotlinHighlightingAttributes
import org.eclipse.jface.preference.IPreferenceStore
import org.jetbrains.kotlin.ui.Activator
import org.eclipse.jdt.internal.ui.preferences.OverlayPreferenceStore.OverlayKey
import org.eclipse.jdt.internal.ui.preferences.OverlayPreferenceStore.*
import org.eclipse.jdt.internal.ui.preferences.OverlayPreferenceStore
import org.eclipse.jface.preference.PreferenceConverter

public class KotlinEditorColoringPreferencePage : PreferencePage(), IWorkbenchPreferencePage {
    private lateinit var treeViewer: TreeViewer
    
    private lateinit var enableCheckBox: Button
    private lateinit var foregroundLabel: Label
    private lateinit var foregroundColor: ColorSelector 
    private lateinit var foregroundColorButton: Button
    private lateinit var boldCheckBox: Button
    private lateinit var italicCheckBox: Button
    private lateinit var underlineCheckBox: Button
    
    private val overlayStore = run {
        val overlayKeys = SEMANTIC_SYNTAX_CATEGORY.elements.flatMap { it -> createOverlayKeys(it) }
        OverlayPreferenceStore(getPreferenceStore(), overlayKeys.toTypedArray())
    }
    
    override fun createContents(parent: Composite): Control? {
        val composite = Composite(parent, SWT.NONE)
        val layout = GridLayout(2, true)
        layout.marginHeight = 0
        layout.marginWidth = 0
        composite.setLayout(layout)
        
        val link = Link(composite, SWT.NONE)
        link.setText(PreferencesMessages.JavaEditorColoringConfigurationBlock_link)
        link.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                PreferencesUtil.createPreferenceDialogOn(parent.getShell(), e.text, null, null)
            }
        })
        link.setLayoutData(GridData(SWT.FILL, SWT.BEGINNING, true, false))
        
        val elementLabel = Label(composite, SWT.LEFT)
        elementLabel.setText(PreferencesMessages.JavaEditorPreferencePage_coloring_element)
        elementLabel.setLayoutData(gridData(horizontalSpan = 2))
        
        createElementsTree(composite)
        
        createStyleGroup(composite)
        
        return composite
    }
    
    override fun performDefaults() {
        super.performDefaults()
        overlayStore.loadDefaults()
        enableColorListSelection()
    }
    
    override fun performOk(): Boolean {
        super.performOk()
        overlayStore.propagate()
        
        return true
    }
    
    override fun init(workbench: IWorkbench?) {
    }
    
    override fun getPreferenceStore(): IPreferenceStore = Activator.getDefault().getPreferenceStore()
    
    override fun dispose() {
        overlayStore.stop()
        super.dispose()
    }
    
    override fun performCancel(): Boolean {
        overlayStore.stop()
        return super.performCancel()
    }
    
    private fun createStyleGroup(composite: Composite) {
        overlayStore.load()
        overlayStore.start()
        
        val stylesGroup = Group(composite, SWT.NONE)
        stylesGroup.setText("Style")
        stylesGroup.setLayout(GridLayout(2, false))
        stylesGroup.setLayoutData(gridData(verticalAlignment = SWT.TOP))
        
        enableCheckBox = Button(stylesGroup, SWT.CHECK)
        enableCheckBox.setText(PreferencesMessages.JavaEditorPreferencePage_enable)
        enableCheckBox.setLayoutData(gridData(horizontalSpan = 2))
        
        foregroundLabel = Label(stylesGroup, SWT.LEFT)
        foregroundLabel.setText(PreferencesMessages.JavaEditorPreferencePage_color)
        foregroundLabel.setLayoutData(gridData())
        
        foregroundColor = ColorSelector(stylesGroup)
        foregroundColorButton = foregroundColor.button
        foregroundColorButton.setLayoutData(gridData())
        
        boldCheckBox = Button(stylesGroup, SWT.CHECK)
        boldCheckBox.setText(PreferencesMessages.JavaEditorPreferencePage_bold)
        boldCheckBox.setLayoutData(gridData(horizontalSpan = 2))
        
        italicCheckBox = Button(stylesGroup, SWT.CHECK)
        italicCheckBox.setText(PreferencesMessages.JavaEditorPreferencePage_italic)
        italicCheckBox.setLayoutData(gridData(horizontalSpan = 2))
    
        underlineCheckBox = Button(stylesGroup, SWT.CHECK)
        underlineCheckBox.setText(PreferencesMessages.JavaEditorPreferencePage_underline)
        underlineCheckBox.setLayoutData(gridData(horizontalSpan = 2))
        
        setUpListeners()
    }
    
    private fun setUpListeners() {
        enableCheckBox.addWidgetSelectionListener { 
            getSelectedKotlinAttribute()?.let { attribute ->
                overlayStore.setValue(attribute.enabledKey, enableCheckBox.selection)
            }
        }
        
        boldCheckBox.addWidgetSelectionListener { 
            getSelectedKotlinAttribute()?.let { attribute ->
                overlayStore.setValue(attribute.boldKey, boldCheckBox.selection)
            }
        }
        
        italicCheckBox.addWidgetSelectionListener { 
            getSelectedKotlinAttribute()?.let { attribute ->
                overlayStore.setValue(attribute.enabledKey, italicCheckBox.selection)
            }
        }
        
        underlineCheckBox.addWidgetSelectionListener { 
            getSelectedKotlinAttribute()?.let { attribute ->
                overlayStore.setValue(attribute.underlineKey, underlineCheckBox.selection)
            }
        }
        
        foregroundColorButton.addWidgetSelectionListener {
            getSelectedKotlinAttribute()?.let { attribute ->
                PreferenceConverter.setValue(overlayStore, attribute.colorKey, foregroundColor.colorValue)
            }
        }
    }
    
    private fun createOverlayKeys(attributes: KotlinHighlightingAttributes): List<OverlayKey> {
        return listOf( 
            OverlayKey(BOOLEAN, attributes.enabledKey),
            OverlayKey(STRING, attributes.colorKey),
            OverlayKey(BOOLEAN, attributes.boldKey),
            OverlayKey(BOOLEAN, attributes.italicKey),
            OverlayKey(BOOLEAN, attributes.underlineKey)
        )
    }
    
    private fun createElementsTree(composite: Composite) {
        treeViewer = TreeViewer(composite, SWT.SINGLE or SWT.BORDER)
        
        val provider = KotlinSyntaxColoringProvider(listOf(SEMANTIC_SYNTAX_CATEGORY))
        treeViewer.setContentProvider(provider)
        treeViewer.setLabelProvider(provider)
        
        treeViewer.getControl().setLayoutData(gridData(
            horizontalAlignment = SWT.FILL,
            verticalAlignment = SWT.FILL,
            grabExcessHorizontalSpace = true,
            grabExcessVerticalSpace = true,
            verticalSpan = 2))
        
        treeViewer.addSelectionChangedListener { 
            enableColorListSelection()
        }
        
        treeViewer.setInput(Any())
    }
    
    private fun enableColorListSelection() {
        val attribute = getSelectedKotlinAttribute()
        if (attribute == null) {
            setAllWidgetsEnabled(false)
        } else {
            foregroundColor.setColorValue(PreferenceConverter.getColor(overlayStore, attribute.colorKey))
            enableCheckBox.setSelection(overlayStore.getBoolean(attribute.enabledKey))
            boldCheckBox.setSelection(overlayStore.getBoolean(attribute.boldKey))
            italicCheckBox.setSelection(overlayStore.getBoolean(attribute.italicKey))
            underlineCheckBox.setSelection(overlayStore.getBoolean(attribute.underlineKey))
            
            setAllWidgetsEnabled(true)
            foregroundColor.getButton().setEnabled(true)
        }
    }
    
    private fun setAllWidgetsEnabled(enabled: Boolean) {
        val widgets = listOf(enableCheckBox, foregroundLabel, foregroundColorButton, 
                boldCheckBox, italicCheckBox, underlineCheckBox)
        widgets.forEach { it.setEnabled(enabled) }
    }
    
    private fun getSelectedKotlinAttribute(): KotlinHighlightingAttributes? {
        val selection = treeViewer.selection
        if (selection is IStructuredSelection) {
            return selection.toArray().firstOrNull() as? KotlinHighlightingAttributes
        }
        
        return null
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
    
    private fun Button.addWidgetSelectionListener(action: (SelectionEvent?) -> Unit) {
        addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent?) {
                action(e)
            }
        })
    }
}