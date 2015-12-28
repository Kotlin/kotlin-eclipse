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

public class KotlinCacheServiceImpl : KotlinCacheService {
    override fun getResolutionFacade(elements: List<KtElement>): ResolutionFacade {
        return KotlinSimpleResolutionFacade()
    }
}

class KotlinSimpleResolutionFacade : ResolutionFacade {
    override val moduleDescriptor: ModuleDescriptor
        get() = throw UnsupportedOperationException()
    
    override val project: Project
        get() = throw UnsupportedOperationException()
    
    override fun analyze(element: KtElement, bodyResolveMode: BodyResolveMode): BindingContext {
        val ktFile = element.getContainingKtFile()
        val javaProject = KotlinPsiManager.getJavaProject(element)
        if (javaProject == null) {
            KotlinLogger.logWarning("JavaProject for $element (in $ktFile) is null")
            return BindingContext.EMPTY
        }
        
        return KotlinAnalysisFileCache.getAnalysisResult(ktFile, javaProject).analysisResult.bindingContext
    }
    
    override fun analyzeFullyAndGetResult(elements: Collection<KtElement>): AnalysisResult {
        throw UnsupportedOperationException()
    }
    
    override fun <T : Any> getFrontendService(element: PsiElement, serviceClass: Class<T>): T {
        throw UnsupportedOperationException()
    }
    
    override fun <T : Any> getFrontendService(serviceClass: Class<T>): T {
        throw UnsupportedOperationException()
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