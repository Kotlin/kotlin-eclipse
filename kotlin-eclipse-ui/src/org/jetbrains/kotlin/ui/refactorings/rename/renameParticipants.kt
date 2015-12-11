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
package org.jetbrains.kotlin.ui.refactorings.rename

import org.eclipse.jdt.ui.search.QuerySpecification
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory
import org.eclipse.jdt.core.search.IJavaSearchConstants
import org.jetbrains.kotlin.ui.commands.findReferences.KotlinOnlyQuerySpecification
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.core.builder.KotlinPsiManager

public class KotlinTypeRenameParticipant : KotlinRenameParticipant()

public class KotlinFunctionRenameParticipant : KotlinRenameParticipant()

public class KotlinLocalRenameParticipant : KotlinRenameParticipant() {
    override fun createSearchQuery(): QuerySpecification {
        val jetElement = element as KtElement
        return KotlinOnlyQuerySpecification(
                jetElement,
                listOf(KotlinPsiManager.getEclipseFile(jetElement.getContainingKtFile())!!), 
                IJavaSearchConstants.ALL_OCCURRENCES,
                JavaSearchScopeFactory.getInstance().getWorkspaceScopeDescription(false))
    }
}
