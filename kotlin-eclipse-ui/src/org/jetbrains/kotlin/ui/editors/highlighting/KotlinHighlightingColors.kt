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
package org.jetbrains.kotlin.ui.editors.highlighting

import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightings
import org.eclipse.swt.graphics.RGB
import org.jetbrains.kotlin.ui.editors.highlighting.KotlinHighlightingAttributes.*
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jdt.ui.PreferenceConstants
import org.eclipse.jface.preference.PreferenceConverter
import org.eclipse.jdt.internal.ui.JavaPlugin

class KotlinHighlightingAttributes(val name: String, val styleKey: String) {
    companion object {
        val KEY_PREFIX = "kotlinSemanticHighlighting."
        val ENABLED_SUFFIX = ".enabled"
        val FOREGROUND_COLOR_SUFFIX = ".color"
        val BOLD_SUFFIX = ".bold"
        val ITALIC_SUFFIX = ".italic"
        val UNDERLINE_SUFFIX = ".underline"
        
        fun withAttributes(
                attributeName: String, 
                attributeKey: String, 
                changeAttributes: KotlinHighlightingAttributes.() -> Unit = { }): KotlinHighlightingAttributes {
            val attributes = KotlinHighlightingAttributes(attributeName, attributeKey)
            attributes.changeAttributes()
            return attributes
        }
        
        val LOCAL_FINAL_VARIABLE = withAttributes("Local final variable", SemanticHighlightings.LOCAL_VARIABLE)
        
        val LOCAL_VARIABLE = withAttributes("Local variable", SemanticHighlightings.LOCAL_VARIABLE) { 
            this.underline = true
        }
        
        val PARAMETER_VARIABLE = withAttributes("Parameter variable", SemanticHighlightings.PARAMETER_VARIABLE)
        
        val FIELD = withAttributes("Field", SemanticHighlightings.FIELD) { 
            this.underline = true
        }
        
        val FINAL_FIELD = withAttributes("Final field", SemanticHighlightings.FIELD)
        
        val STATIC_FIELD = withAttributes("Static field", SemanticHighlightings.STATIC_FIELD) { 
            this.underline = true
        }
        
        val STATIC_FINAL_FIELD = withAttributes("Static final field", SemanticHighlightings.STATIC_FINAL_FIELD)
        
        val TYPE_PARAMETER = withAttributes("Type parameter", SemanticHighlightings.TYPE_VARIABLE)
        
        val ANNOTATION = withAttributes("Annotation", SemanticHighlightings.ANNOTATION)
        
        val ENUM_CLASS = withAttributes("Enum class", SemanticHighlightings.ENUM)
        
        val INTERFACE = withAttributes("Interface", SemanticHighlightings.INTERFACE)
        
        val CLASS = withAttributes("Class declaration", SemanticHighlightings.CLASS)
        
        val FUNCTION_DECLARATION = withAttributes("Function declaration", SemanticHighlightings.METHOD_DECLARATION)
    }
    
    var underline: Boolean? = null
    
    val boldKey = buildKey(BOLD_SUFFIX)
    val italicKey = buildKey(ITALIC_SUFFIX)
    val enabledKey = buildKey(ENABLED_SUFFIX)
    val underlineKey = buildKey(UNDERLINE_SUFFIX)
    val colorKey = buildKey(FOREGROUND_COLOR_SUFFIX)
    
    fun getColor(store: IPreferenceStore): RGB = PreferenceConverter.getColor(store, colorKey)
    fun isBold(store: IPreferenceStore): Boolean = store.getBoolean(boldKey)
    fun isItalic(store: IPreferenceStore): Boolean = store.getBoolean(italicKey)
    fun isEnabled(store: IPreferenceStore): Boolean = store.getBoolean(enabledKey)
    fun isUnderline(store: IPreferenceStore): Boolean = store.getBoolean(underlineKey)
    
    
    private fun buildKey(suffix: String): String = "${KEY_PREFIX}${styleKey}${suffix}"
}

class KotlinSyntaxCategory(val name: String, val elements: List<KotlinHighlightingAttributes>)

val SEMANTIC_SYNTAX_CATEGORY = createSyntaxCategory("Kotlin Semantic Highlighting") {
    listOf(LOCAL_FINAL_VARIABLE, LOCAL_VARIABLE, PARAMETER_VARIABLE, FIELD, FINAL_FIELD, STATIC_FIELD,
        STATIC_FINAL_FIELD, TYPE_PARAMETER, ANNOTATION, ENUM_CLASS, INTERFACE, CLASS, FUNCTION_DECLARATION)
}

fun initializePreferences(store: IPreferenceStore) {
    initializePreferencesByJavaValues(store, JavaPlugin.getDefault().getPreferenceStore())
    
    SEMANTIC_SYNTAX_CATEGORY.elements.forEach {
        if (it.underline != null) store.setDefault(it.underlineKey, it.underline!!)
    }
}

private fun initializePreferencesByJavaValues(kotlinStore: IPreferenceStore, javaStore: IPreferenceStore) {
    SEMANTIC_SYNTAX_CATEGORY.elements.forEach {
        PreferenceConverter.setDefault(kotlinStore, it.colorKey, getDefaultColor(it.styleKey, javaStore))
        kotlinStore.setDefault(it.boldKey, isDefaultBold(it.styleKey, javaStore))
        kotlinStore.setDefault(it.italicKey, isDefaultItalic(it.styleKey, javaStore))
        kotlinStore.setDefault(it.enabledKey, isDefaultEnabled(it.styleKey, javaStore))
        kotlinStore.setDefault(it.underlineKey, isDefaultUnderline(it.styleKey, javaStore))
    }
}

private fun createSyntaxCategory(
        name: String, 
        createElements: KotlinHighlightingAttributes.Companion.() -> List<KotlinHighlightingAttributes>): KotlinSyntaxCategory {
    return KotlinSyntaxCategory(name, with(KotlinHighlightingAttributes) { createElements() } )
}

private fun getDefaultColor(key: String, store: IPreferenceStore): RGB {
    val preferenceKey = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + key + 
            PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_COLOR_SUFFIX
    
    return PreferenceConverter.getDefaultColor(store, preferenceKey)
}

private fun isDefaultBold(key: String, store: IPreferenceStore): Boolean {
    val preferenceKey = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + key + 
            PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_BOLD_SUFFIX
    
    return store.getDefaultBoolean(preferenceKey)
}

private fun isDefaultItalic(key: String, store: IPreferenceStore): Boolean {
    val preferenceKey = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + key + 
            PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ITALIC_SUFFIX
    
    return store.getDefaultBoolean(preferenceKey)
}

private fun isDefaultEnabled(key: String, store: IPreferenceStore): Boolean {
    val preferenceKey = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + key + 
            PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED_SUFFIX
    
    return store.getDefaultBoolean(preferenceKey)
}

private fun isDefaultUnderline(key: String, store: IPreferenceStore): Boolean {
    val preferenceKey = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + key + 
            PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_UNDERLINE_SUFFIX
        
    return store.getDefaultBoolean(preferenceKey)
}