package org.jetbrains.kotlin.testframework.utils

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.ProjectScope
import org.jetbrains.kotlin.core.formatting.KotlinCodeStyleManager
import org.jetbrains.kotlin.core.preferences.KotlinCodeStyleProperties
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import java.util.*
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty

object CodeStyleConfigurator {
    fun configure(project: IProject, fileText: String) {
        val generatedId = UUID.randomUUID().toString()

        KotlinCodeStyleProperties(ProjectScope(project)).apply {
            codeStyleId = generatedId
            globalsOverridden = true
            saveChanges()
        }

        KotlinCodeStyleManager.getOrCreate(generatedId) {
            val kotlinSettings = getCustomSettings(KotlinCodeStyleSettings::class.java)
            val commonSettings = getCommonSettings(KotlinLanguage.INSTANCE)

            fun setDynamic(prop: String, value: Any) {
                kotlinSettings.setDynamic(prop, value) or commonSettings.setDynamic(prop, value)
            }

            InTextDirectivesUtils.findListWithPrefixes(fileText, "SET_TRUE:")
                    .forEach { setDynamic(it, true) }

            InTextDirectivesUtils.findListWithPrefixes(fileText, "SET_FALSE:")
                    .forEach { setDynamic(it, false) }

            InTextDirectivesUtils.findListWithPrefixes(fileText, "SET_INT:")
                    .map { it.split("=", limit = 2) }
                    .forEach { (prop, value) -> setDynamic(prop, value.trim().toInt()) }

            InTextDirectivesUtils.findStringWithPrefixes(fileText, "RIGHT_MARGIN: ")
                    ?.also { commonSettings.RIGHT_MARGIN = it.trim().toInt() }
        }
    }

    fun deconfigure(project: IProject) {
        KotlinCodeStyleProperties(ProjectScope(project)).apply {
            globalsOverridden = false
            codeStyleId?.also { KotlinCodeStyleManager.invalidate(it) }
            saveChanges()
        }
    }

    private fun Any.setDynamic(name: String, value: Any): Boolean =
            this::class.members.singleOrNull { it.name in setOf(name) }
                    ?.let {
                        when (it) {
                            is KMutableProperty -> it.setter
                            is KFunction -> it
                            else -> null
                        }
                    }
                    ?.call(this, value)
                    ?.let { true }
                    ?: false

}