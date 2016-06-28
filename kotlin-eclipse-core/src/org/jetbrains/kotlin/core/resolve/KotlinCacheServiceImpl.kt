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
package org.jetbrains.kotlin.core.resolve

import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.analyzer.AnalysisResult
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.core.log.KotlinLogger
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache
import org.jetbrains.kotlin.resolve.diagnostics.KotlinSuppressCache
import org.jetbrains.kotlin.core.model.KotlinEnvironment
import org.jetbrains.kotlin.core.model.getEclipseResource
import org.jetbrains.kotlin.core.model.getEnvironment
import org.jetbrains.kotlin.container.ComponentProvider
import java.lang.IllegalStateException

public class KotlinCacheServiceImpl(val ideaProject: Project) : KotlinCacheService {
    override fun getSuppressionCache(): KotlinSuppressCache {
        throw UnsupportedOperationException()
    }

    override fun getResolutionFacade(elements: List<KtElement>): ResolutionFacade {
        return KotlinSimpleResolutionFacade(ideaProject, elements)
    }
}

class KotlinSimpleResolutionFacade(
        override val project: Project,
        private val elements: List<KtElement>) : ResolutionFacade {
    override val moduleDescriptor: ModuleDescriptor
        get() = throw UnsupportedOperationException()
    
    override fun analyze(element: KtElement, bodyResolveMode: BodyResolveMode): BindingContext {
        val ktFile = element.getContainingKtFile()
        return KotlinAnalysisFileCache.getAnalysisResult(ktFile).analysisResult.bindingContext
    }
    
    override fun analyzeFullyAndGetResult(elements: Collection<KtElement>): AnalysisResult {
        throw UnsupportedOperationException()
    }
    
    override fun <T : Any> getFrontendService(element: PsiElement, serviceClass: Class<T>): T {
        throw UnsupportedOperationException()
    }
    
    override fun <T : Any> getFrontendService(serviceClass: Class<T>): T {
        val files = elements.map { it.getContainingKtFile() }.toSet()
        if (files.isEmpty()) throw IllegalStateException("Elements should not be empty")
        
        val environment = getEnvironment(project) ?:
            throw IllegalStateException("Kotlin environment for idea project ($project) should not be null")
        
        val componentProvider = KotlinAnalyzer.analyzeFiles(files, environment).componentProvider
        
        return componentProvider.getService(serviceClass)
    }
    
    override fun <T : Any> getFrontendService(moduleDescriptor: ModuleDescriptor, serviceClass: Class<T>): T {
        throw UnsupportedOperationException()
    }
    
    override fun <T : Any> getIdeService(serviceClass: Class<T>): T {
        throw UnsupportedOperationException()
    }
    
    override fun resolveToDescriptor(declaration: KtDeclaration): DeclarationDescriptor {
        throw UnsupportedOperationException()
    }
}

@Suppress("UNCHECKED_CAST") fun <T : Any> ComponentProvider.getService(request: Class<T>): T {
    return resolve(request)!!.getValue() as T
}