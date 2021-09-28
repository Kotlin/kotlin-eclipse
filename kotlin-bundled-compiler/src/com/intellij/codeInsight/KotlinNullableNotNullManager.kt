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
package com.intellij.codeInsight

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierListOwner
import org.jetbrains.annotations.Nullable


// Dummy implementation. Will be changed to something more useful, when KE-277 is fixed.
class KotlinNullableNotNullManager(project: Project) : NullableNotNullManager(project) {
    private val _nullables = mutableListOf<String>()
    private val _notNulls = mutableListOf<String>()

    override fun getNullables(): List<String> = _nullables

    override fun setInstrumentedNotNulls(names: MutableList<String>) {}

    override fun getInstrumentedNotNulls(): List<String> = emptyList()

//    override fun isJsr305Default(annotation: PsiAnnotation, placeTargetTypes: Array<out PsiAnnotation.TargetType>): NullabilityAnnotationInfo? = null

    override fun setNullables(vararg annotations: String) {
        _nullables.clear()
        _nullables.addAll(annotations)
    }

    override fun getNotNulls(): List<String> = _notNulls

    override fun setDefaultNotNull(defaultNotNull: String) {
    }

    override fun getDefaultNullable(): String = "Nullable"

    override fun getDefaultNotNull(): String {
        return "NotNullable"
    }

    override fun getDefaultNullables(): MutableList<String> {
        return mutableListOf(defaultNullable)
    }

    override fun getDefaultNotNulls(): MutableList<String> {
        return mutableListOf(defaultNotNull)
    }

    override fun getAllDefaultAnnotations(): MutableList<String> {
        return (defaultNullables + defaultNotNulls).toMutableList()
    }

    override fun setNotNulls(vararg annotations: String) {
        _notNulls.clear()
        _notNulls.addAll(annotations)
    }

    //    For now we get unresolved psi elements and as a result annotations qualified names are short
    init {
        setNotNulls("NotNull")
        setNullables("Nullable")
    }

    override fun setDefaultNullable(defaultNullable: String) {
    }

    override fun hasHardcodedContracts(element: PsiElement): Boolean = false

    override fun isNotNull(owner: PsiModifierListOwner, checkBases: Boolean): Boolean {
        val notNullAnnotations = notNulls.toSet()
        return owner.modifierList?.annotations?.any { annotation ->
            annotation.qualifiedName in notNullAnnotations
        } ?: false
    }

    @Nullable
    override fun getNullityDefault(
        container: PsiModifierListOwner,
        placeTargetTypes: Array<PsiAnnotation.TargetType>,
        context: PsiElement, superPackage: Boolean
    ): NullabilityAnnotationInfo? {
        return null
    }

    override fun isNullable(owner: PsiModifierListOwner, checkBases: Boolean) = !isNotNull(owner, checkBases)
}