package org.jetbrains.kotlin.tools

import org.eclipse.core.commands.AbstractHandler
import org.eclipse.core.commands.ExecutionEvent
import org.eclipse.core.commands.IHandler
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.ui.ISources
import org.eclipse.ui.handlers.HandlerUtil
import org.jetbrains.kotlin.core.compiler.KotlinCompiler
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class ForceRecompile : AbstractHandler(), IHandler {
    override fun execute(event: ExecutionEvent?) {
        val project = findJavaProject(event?.applicationContext) ?: return
            workspaceJob(name = "Recompiling ${project.project.name}", resource = project.project) {
                KotlinCompiler.INSTANCE.compileKotlinFiles(project)
            }
    }

    override fun setEnabled(evaluationContext: Any?) {
        setBaseEnabled(findJavaProject(evaluationContext) is Any)
    }

    private fun findJavaProject(context: Any?): IJavaProject? =
        (HandlerUtil.getVariable(context, ISources.ACTIVE_CURRENT_SELECTION_NAME) as? IStructuredSelection)
            ?.toArray()
            ?.firstIsInstanceOrNull()

}