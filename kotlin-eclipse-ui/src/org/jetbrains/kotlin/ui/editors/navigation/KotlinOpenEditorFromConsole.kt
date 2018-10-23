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
import org.eclipse.jdt.core.IType
import org.eclipse.core.resources.IFile
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil
import org.eclipse.jface.text.BadLocationException
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.ui.navigation.KotlinOpenEditor
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.core.utils.ProjectUtils

private val NAVIGATION_DESCRIPTION_REGEX = "at (.+)\\((.+\\.kt)\\:(\\d+)\\)".toRegex()

class KotlinOpenEditorFromConsole : IPatternMatchListenerDelegate {
    private @Volatile var console: TextConsole? = null
    
    override fun connect(console: TextConsole) {
        this.console = console
    }    
    
    override fun disconnect() {
        this.console = null
    }
    
    @Suppress("UNUSED_VARIABLE")
    override fun matchFound(event: PatternMatchEvent) {
        val linkText = console?.document?.get(event.offset, event.length)
        if (linkText == null) return
        
        val navigationDescription = NAVIGATION_DESCRIPTION_REGEX.find(linkText)?.groupValues
        if (navigationDescription == null) return
        
        val (allDescription, fqName, fileName, lineNumber) = navigationDescription
        
        val linkOffset = event.offset + 4 + fqName.length // at (linkOffset)
        val linkLength = fileName.length + 1 + lineNumber.length // link:link
        
        if (canNavigate(fqName, lineNumber)) {
            console?.addHyperlink(object : IHyperlink {
                override fun linkActivated() {
                    openEditor(fqName, lineNumber)
                }
                
                override fun linkExited() {
                }
                
                override fun linkEntered() {
                }
            }, linkOffset, linkLength)
        }
    }
    
    private fun canNavigate(fqName: String, lineNumber: String): Boolean {
        val type = findType(fqName)
        if (type == null) return false
        
        val kotlinSourceFiles = KotlinOpenEditor.findSourceFiles(type)
        if (kotlinSourceFiles.size != 1) return false
        
        val resource = KotlinPsiManager.getEclipseFile(kotlinSourceFiles.first())
        if (resource == null) return false
        
        val document = EditorUtil.getDocument(resource)
        try {
            val lineRegion = document.getLineInformation(lineNumber.toInt() - 1)
            if (lineRegion.offset > document.length) {
                return false
            }
        } catch (e: BadLocationException) {
            return false
        }
        
        return true
    }
    
    private fun openEditor(fqName: String, lineNumber: String) {
        val type = findType(fqName)
        if (type == null) return
        
        try {
            val editorPart = EditorUtility.openInEditor(type, true)
            if (editorPart is KotlinFileEditor) {
                val lineRegion = editorPart.document.getLineInformation(lineNumber.toInt() - 1)
                editorPart.selectAndReveal(lineRegion.offset, 0)
            }
        } catch (e: BadLocationException) {
//            As we've already validated that we can navigate to that line number,
//            this exception may occur only if some lines or file have been deleted
        }
        
        return
    }
    
    private fun findType(fqName: String): IType? {
        val typeFqName = fqName.substringBeforeLast(".")
        return ProjectUtils.accessibleKotlinProjects.asSequence()
                .map { JavaCore.create(it) }
                .mapNotNull { it.findType(typeFqName) }
                .firstOrNull()
    }
}