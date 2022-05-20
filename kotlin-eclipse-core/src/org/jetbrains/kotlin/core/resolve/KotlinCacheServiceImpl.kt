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
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.KotlinSuppressCache
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.analyzer.ResolverForProject
import org.jetbrains.kotlin.caches.resolve.PlatformAnalysisSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.idea.FrontendInternals

class KotlinCacheServiceImpl(private val ideaProject: Project) : KotlinCacheService {

    override fun getResolutionFacadeWithForcedPlatform(
        elements: List<KtElement>,
        platform: TargetPlatform
    ): ResolutionFacade {
        return KotlinSimpleResolutionFacade(ideaProject, elements)
    }

    override fun getResolutionFacadeByFile(file: PsiFile, platform: TargetPlatform): ResolutionFacade {
        throw UnsupportedOperationException()
    }

    override fun getResolutionFacadeByModuleInfo(
        moduleInfo: ModuleInfo,
        settings: PlatformAnalysisSettings
    ): ResolutionFacade? {
        throw UnsupportedOperationException()
    }

    override fun getSuppressionCache(): KotlinSuppressCache {
        throw UnsupportedOperationException()
    }

    override fun getResolutionFacade(elements: List<KtElement>): ResolutionFacade {
        return KotlinSimpleResolutionFacade(ideaProject, elements)
    }

    override fun getResolutionFacade(element: KtElement): ResolutionFacade = getResolutionFacade(listOf(element))

	override fun getResolutionFacadeByModuleInfo(moduleInfo: ModuleInfo, platform: TargetPlatform): ResolutionFacade? =
		null
}

@OptIn(FrontendInternals::class)
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
        val ktFile = element.containingKtFile
        return KotlinAnalysisFileCache.getAnalysisResult(ktFile).analysisResult.bindingContext
    }

    override fun analyzeWithAllCompilerChecks(
        elements: Collection<KtElement>,
        callback: DiagnosticSink.DiagnosticsCallback?
    ): AnalysisResult {
        val ktFile = elements.first().containingKtFile
        return KotlinAnalysisFileCache.getAnalysisResult(ktFile).analysisResult
    }

    override fun analyze(elements: Collection<KtElement>, bodyResolveMode: BodyResolveMode): BindingContext {
        if (elements.isEmpty()) {
            return BindingContext.EMPTY
        }
        val ktFile = elements.first().containingKtFile
        return KotlinAnalysisFileCache.getAnalysisResult(ktFile).analysisResult.bindingContext
    }

    override fun <T : Any> getFrontendService(element: PsiElement, serviceClass: Class<T>): T {
        throw UnsupportedOperationException()
    }
    
    override fun <T : Any> getFrontendService(serviceClass: Class<T>): T {
        val files = elements.map { it.containingKtFile }.toSet()
        if (files.isEmpty()) throw IllegalStateException("Elements should not be empty")
        
        val componentProvider = KotlinAnalyzer.analyzeFiles(files).componentProvider
            ?: throw IllegalStateException("Trying to get service from non-initialized project")

        return componentProvider.getService(serviceClass)
    }
    
    override fun <T : Any> getFrontendService(moduleDescriptor: ModuleDescriptor, serviceClass: Class<T>): T {
        throw UnsupportedOperationException()
    }
    
    override fun <T : Any> getIdeService(serviceClass: Class<T>): T {
        throw UnsupportedOperationException()
    }

    override fun getResolverForProject(): ResolverForProject<out ModuleInfo> {
        throw UnsupportedOperationException()
    }
}

@Suppress("UNCHECKED_CAST") fun <T : Any> ComponentProvider.getService(request: Class<T>): T {
    return resolve(request)!!.getValue() as T
}