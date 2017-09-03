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

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.diagnostics.KotlinSuppressCache
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

public class KotlinCacheServiceImpl(val ideaProject: Project) : KotlinCacheService {
    override fun getResolutionFacadeByFile(file: PsiFile, platform: TargetPlatform): ResolutionFacade {
        throw UnsupportedOperationException()
    }

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
    override fun <T : Any> tryGetFrontendService(element: PsiElement, serviceClass: Class<T>): T? {
        return null
    }

    override fun resolveToDescriptor(declaration: KtDeclaration, bodyResolveMode: BodyResolveMode): DeclarationDescriptor {
        throw UnsupportedOperationException()
    }

    override val moduleDescriptor: ModuleDescriptor
        get() = throw UnsupportedOperationException()
    
    override fun analyze(element: KtElement, bodyResolveMode: BodyResolveMode): BindingContext {
        val ktFile = element.getContainingKtFile()
        return KotlinAnalysisFileCache.getAnalysisResult(ktFile).analysisResult.bindingContext
    }
    
    override fun analyze(elements: Collection<KtElement>, bodyResolveMode: BodyResolveMode): BindingContext {
        if (elements.isEmpty()) {
            return BindingContext.EMPTY
        }
        val ktFile = elements.first().getContainingKtFile()
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
        
        val componentProvider = KotlinAnalyzer.analyzeFiles(files).componentProvider
        
        return componentProvider.getService(serviceClass)
    }
    
    override fun <T : Any> getFrontendService(moduleDescriptor: ModuleDescriptor, serviceClass: Class<T>): T {
        throw UnsupportedOperationException()
    }
    
    override fun <T : Any> getIdeService(serviceClass: Class<T>): T {
        throw UnsupportedOperationException()
    }
}

@Suppress("UNCHECKED_CAST") fun <T : Any> ComponentProvider.getService(request: Class<T>): T {
    return resolve(request)!!.getValue() as T
}