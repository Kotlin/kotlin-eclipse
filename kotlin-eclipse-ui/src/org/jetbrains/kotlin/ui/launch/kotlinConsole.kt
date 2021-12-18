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
package org.jetbrains.kotlin.ui.launch

import org.eclipse.jdt.core.IJavaProject
import org.eclipse.ui.console.ConsolePlugin
import org.eclipse.ui.console.MessageConsole

private const val KOTLIN_CONSOLE_ID = "org.jetbrains.kotlin.ui.console"

private val manager get() = ConsolePlugin.getDefault().consoleManager

private val IJavaProject.consoleName get() = "$KOTLIN_CONSOLE_ID.${project.name}"

fun createCleanKotlinConsole(javaProject: IJavaProject): MessageConsole {
    removeKotlinConsoles(javaProject)

    return createNewKotlinConsole(javaProject)
}

private fun createNewKotlinConsole(javaProject: IJavaProject): MessageConsole {
    val messageConsole = MessageConsole(javaProject.consoleName, null)
    manager.addConsoles(arrayOf(messageConsole))

    return messageConsole
}

fun removeKotlinConsoles(javaProject: IJavaProject) {
    manager.removeConsoles(manager.consoles
            .filter { it.name == javaProject.consoleName }
            .toTypedArray())
}