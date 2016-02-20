/*******************************************************************************

* Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.kotlin.ui.editors.navigation

import org.eclipse.ui.console.IPatternMatchListenerDelegate
import org.eclipse.ui.console.PatternMatchEvent
import org.eclipse.ui.console.TextConsole
import org.eclipse.ui.console.IHyperlink
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor

private val NAVIGATION_DESCRIPTION_REGEX = "at (.+)\\((.+\\.kt)\\:(\\d+)\\)".toRegex()

class KotlinOpenEditorFromConsole : IPatternMatchListenerDelegate {
    private @Volatile var console: TextConsole? = null
    
    override fun connect(console: TextConsole) {
        this.console = console
    }    
    
    override fun disconnect() {
        this.console = null
    }
    
    override fun matchFound(event: PatternMatchEvent) {
        val linkText = console?.document?.get(event.offset, event.length)
        if (linkText == null) return
        
        val navigationDescription = NAVIGATION_DESCRIPTION_REGEX.find(linkText)?.groupValues
        if (navigationDescription == null) return
        
        val (allDescription, fqName, fileName, lineNumber) = navigationDescription
        
        console?.addHyperlink(object : IHyperlink {
            override fun linkActivated() {
                openEditor(fqName, lineNumber)
            }
            
            override fun linkExited() {
            }
            
            override fun linkEntered() {
            }
        }, event.offset + 4 + fqName.length, fileName.length + 1 + lineNumber.length)
    }
    
    fun openEditor(fqName: String, lineNumber: String) {
        val typeFqName = fqName.substringBeforeLast(".")
        for (project in ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
            val javaProject = JavaCore.create(project)
            val javaType = javaProject.findType(typeFqName)
            if (javaType != null) {
                val editorPart = EditorUtility.openInEditor(javaType, true)
                if (editorPart is KotlinFileEditor) {
                    val lineRegion = editorPart.document.getLineInformation(lineNumber.toInt() - 1)
                    editorPart.selectAndReveal(lineRegion.offset, 0)
                }
                
                return
            }
        }
    }
}