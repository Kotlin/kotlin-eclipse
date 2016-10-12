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
 *******************************************************************************/
package org.jetbrains.kotlin.wizards

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtToken

enum class WizardType(val wizardTypeName: String, val fileBodyFormat: String = "") {
    NONE("Source"),
    CLASS("Class", buildFileBody(KtTokens.CLASS_KEYWORD)),
    INTERFACE("Interface", buildFileBody(KtTokens.INTERFACE_KEYWORD)),
    OBJECT("Object", buildFileBody(KtTokens.OBJECT_KEYWORD)),
    ENUM("Enum", buildFileBody(KtTokens.ENUM_KEYWORD, KtTokens.CLASS_KEYWORD))
}

private val NOT_EMPTY_BODY_FORMAT = "%s {\n}"

private fun buildFileBody(vararg modifiers: KtToken): String =
        "${modifiers.joinToString(separator = " ")} $NOT_EMPTY_BODY_FORMAT"