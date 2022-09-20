/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
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
 */
package org.jetbrains.kotlin.ui.launch

import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IStatus
import org.eclipse.debug.core.IStatusHandler
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.graphics.RGB
import org.eclipse.ui.console.ConsolePlugin
import org.eclipse.ui.console.MessageConsole
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.core.compiler.KotlinCompilerUtils
import org.jetbrains.kotlin.core.launch.CompilerOutputElement

class CompilerStatusHandler : IStatusHandler {

    @Throws(CoreException::class)
    override fun handleStatus(status: IStatus, source: Any): Any? {
        if (source !is KotlinCompilerUtils.CompilerOutputWithProject) {
            return null
        }
        val javaProject: IJavaProject = source.project
        val outputDataList: List<CompilerOutputElement> = source.data.list
        val sortedOutput = groupOutputByPath(outputDataList)
        val msgConsole = createCleanKotlinConsole(javaProject)
        for (outputList in sortedOutput.values) {
            printCompilerOutputList(outputList, msgConsole)
        }
        if (status.severity == IStatus.ERROR) {
            ConsolePlugin.getDefault().consoleManager.showConsoleView(msgConsole)
        }
        return null
    }

    private fun printCompilerOutputList(outputList: List<CompilerOutputElement>, msgConsole: MessageConsole) {
        val path = outputList[0].messageLocation?.path
        msgConsole.println(path ?: "No Location", CONSOLE_BLACK)
        for (dataElement in outputList) {
            val color = dataElement.messageSeverity.color
            val message = StringBuilder()
            message.append("\t")
            message.append(dataElement.messageSeverity.toString() + ": " + dataElement.message)
            if (dataElement.messageLocation != null) {
                message.append(" (" + dataElement.messageLocation.line + ", " + dataElement.messageLocation.column + ")")
            }
            msgConsole.println(message.toString(), color)
        }
    }

    private val CompilerMessageSeverity.color: RGB
        get() = when (this) {
            CompilerMessageSeverity.ERROR -> CONSOLE_RED
            CompilerMessageSeverity.WARNING -> CONSOLE_YELLOW
            else -> CONSOLE_BLACK
        }

    private fun MessageConsole.println(message: String, color: RGB?) = newMessageStream().apply {
        this.color = Color(null, color)
        println(message)
    }

    private fun groupOutputByPath(outputData: List<CompilerOutputElement>): Map<String, MutableList<CompilerOutputElement>> {
        val res: MutableMap<String, MutableList<CompilerOutputElement>> = HashMap()
        val emptyPath = ""
        for (dataElement in outputData) {
            val path = dataElement.messageLocation?.path ?: emptyPath
            if (!res.containsKey(path)) {
                res[path] = ArrayList()
            }
            res[path]!!.add(dataElement)
        }
        return res
    }

    companion object {
        private val CONSOLE_RED = RGB(229, 43, 80)
        private val CONSOLE_YELLOW = RGB(218, 165, 32)
        private val CONSOLE_BLACK = RGB(0, 0, 0)
    }
}