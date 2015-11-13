package org.jetbrains.kotlin.ui.editors.highlighting

import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightings
import org.eclipse.swt.graphics.RGB

public class KotlinHighlightingColors {
    companion object {
        val ANNOTATION = SemanticHighlightings.ANNOTATION
        val LOCAL_VARIABLE = SemanticHighlightings.LOCAL_VARIABLE
        val PARAMETER_VARIABLE = SemanticHighlightings.PARAMETER_VARIABLE
    }
}

class KotlinHighlightingAttributes private constructor() {
    companion object {
        fun withAttributes(changeAttributes: KotlinHighlightingAttributes.() -> Unit): KotlinHighlightingAttributes {
            val attributes = KotlinHighlightingAttributes()
            attributes.changeAttributes()
            return attributes
        }
        
        val LOCAL_FINAL_VARIABLE = withAttributes { 
            this.colorKey = SemanticHighlightings.LOCAL_VARIABLE
        }
        
        val LOCAL_VARIABLE = withAttributes { 
            this.colorKey = SemanticHighlightings.LOCAL_VARIABLE
            this.underline = true
        }
        
        val PARAMETER_VARIABLE = withAttributes { 
            this.colorKey = SemanticHighlightings.PARAMETER_VARIABLE
        }
        
        val FIELD = withAttributes { 
            this.colorKey = SemanticHighlightings.FIELD
            this.underline = true
        }
        
        val FINAL_FIELD = withAttributes { 
            this.colorKey = SemanticHighlightings.FIELD
        }
        
        val STATIC_FIELD = withAttributes { 
            this.colorKey = SemanticHighlightings.STATIC_FIELD
            this.italic = true
            this.underline = true
        }
        
        val STATIC_FINAL_FIELD = withAttributes { 
            this.colorKey = SemanticHighlightings.STATIC_FINAL_FIELD
            this.italic = true
            this.bold = true
        }
        
        val SMART_CAST_VALUE = RGB(219, 255, 219) 
    }
    
    lateinit var colorKey: String
    var underline = false
    var bold = false
    var italic = false
}