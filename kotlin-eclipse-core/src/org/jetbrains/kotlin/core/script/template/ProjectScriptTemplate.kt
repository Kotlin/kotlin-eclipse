package org.jetbrains.kotlin.core.script.template

import kotlin.script.templates.ScriptTemplateDefinition


@ScriptTemplateDefinition(resolver = ProjectFilesResolver::class)
abstract class ProjectScriptTemplate(val args: Array<String>)