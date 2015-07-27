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
package org.jetbrains.kotlin.core.utils

import org.jetbrains.kotlin.psi.JetElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.name.FqName

fun getDeclaringTypeFqName(jetElement: JetElement): FqName? {
    val parent = PsiTreeUtil.getParentOfType(jetElement, javaClass<JetClassOrObject>(), javaClass<JetFile>())
    
    return when (parent) {
        is JetClassOrObject -> parent.getFqName()
        is JetFile -> PackageClassUtils.getPackageClassFqName(parent.getPackageFqName())
        else -> null
    }
}