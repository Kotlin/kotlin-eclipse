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
    }
    
    lateinit var colorKey: String
    var underline = false
    var bold = false
    var italic = false
}