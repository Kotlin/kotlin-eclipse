package org.jetbrains.kotlin.testframework.utils

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.ProjectScope
import org.jetbrains.kotlin.core.formatting.KotlinCodeStyleManager
import org.jetbrains.kotlin.core.preferences.KotlinCodeStyleProperties
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

            InTextDirectivesUtils.findListWithPrefixes(fileText, "SET_TRUE:")
                    .forEach { kotlinSettings[it] = true }

            InTextDirectivesUtils.findListWithPrefixes(fileText, "SET_FALSE:")
                    .forEach { kotlinSettings[it] = false }

            InTextDirectivesUtils.findListWithPrefixes(fileText, "SET_INT:")
                    .map { it.split("=", limit = 2) }
                    .forEach { (prop, value) -> kotlinSettings[prop] = value.toInt() }
        }
    }

    fun deconfigure(project: IProject) {
        KotlinCodeStyleProperties(ProjectScope(project)).apply {
            globalsOverridden = false
            codeStyleId?.also { KotlinCodeStyleManager.invalidate(it) }
            saveChanges()
        }
    }

    private operator fun Any.set(name: String, value: Any) {
        this::class.members.single { it.name in setOf(name) }.let {
            when (it) {
                is KMutableProperty -> it.setter.call(this, value)
                is KFunction -> it.call(this, value)
                else -> throw AssertionError("Field or method with name $name does not exist")
            }
        }
    }
}