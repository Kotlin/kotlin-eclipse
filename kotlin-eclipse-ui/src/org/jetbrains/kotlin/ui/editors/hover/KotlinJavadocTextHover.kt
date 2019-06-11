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
package org.jetbrains.kotlin.ui.editors.hover

import org.eclipse.jdt.internal.ui.JavaPlugin
import org.eclipse.jdt.internal.ui.JavaPluginImages
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocHover
import org.eclipse.jdt.internal.ui.text.java.hover.JavadocHover.loadStyleSheet
import org.eclipse.jdt.internal.ui.text.javadoc.JavadocContentAccess2
import org.eclipse.jface.action.ToolBarManager
import org.eclipse.jface.internal.text.html.BrowserInformationControl
import org.eclipse.jface.internal.text.html.BrowserInformationControlInput
import org.eclipse.jface.internal.text.html.HTMLPrinter
import org.eclipse.jface.resource.ColorRegistry
import org.eclipse.jface.resource.JFaceResources
import org.eclipse.jface.text.DefaultInformationControl
import org.eclipse.jface.text.IInformationControl
import org.eclipse.jface.text.IInformationControlCreator
import org.eclipse.jface.text.ITextHoverExtension
import org.eclipse.swt.graphics.RGB
import org.eclipse.swt.widgets.Shell
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache
import org.jetbrains.kotlin.core.references.getReferenceExpression
import org.jetbrains.kotlin.core.references.resolveToSourceDeclaration
import org.jetbrains.kotlin.core.resolve.lang.java.resolver.EclipseJavaSourceElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind.ANNOTATION_CLASS
import org.jetbrains.kotlin.descriptors.ClassKind.CLASS
import org.jetbrains.kotlin.descriptors.ClassKind.ENUM_CLASS
import org.jetbrains.kotlin.descriptors.ClassKind.ENUM_ENTRY
import org.jetbrains.kotlin.descriptors.ClassKind.INTERFACE
import org.jetbrains.kotlin.descriptors.ClassKind.OBJECT
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.idea.kdoc.findKDoc
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaPackageFragment
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext.REFERENCE_TARGET
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtensionProperty
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.ui.editors.navigation.findDeclarationInParsedFile
import org.jetbrains.kotlin.ui.editors.navigation.getKtFileFromElement
import org.jetbrains.kotlin.ui.editors.quickassist.resolveToDescriptor
import java.net.URL

class KotlinJavadocTextHover : KotlinEditorTextHover<BrowserInformationControlInput>, ITextHoverExtension {

    class KotlinInformationControlCreator : IInformationControlCreator {

        override fun createInformationControl(parent: Shell?): IInformationControl =
            if (BrowserInformationControl.isAvailable(parent)) {
                val tbm = ToolBarManager(8388608)
                val font = "org.eclipse.jdt.ui.javadocfont"
                BrowserInformationControl(parent, font, tbm)
            } else {
                DefaultInformationControl(parent, true)
            }
    }

    private val fHoverControlCreator: JavadocHover.HoverControlCreator by lazy {
        JavadocHover.HoverControlCreator(KotlinInformationControlCreator())
    }

    override val hoverPriority: Int
        get() = 2

    override fun getHoverControlCreator(): IInformationControlCreator = fHoverControlCreator

    override fun getHoverInfo(hoverData: HoverData): BrowserInformationControlInput? {
        val (element, _) = hoverData
        return KotlinAnnotationBrowserInformationControlInput(element)
    }

    override fun isAvailable(hoverData: HoverData): Boolean = true

    override fun getHoverControlCreator(editor: KotlinEditor): IInformationControlCreator? =
        JavadocHover.PresenterControlCreator(editor.javaEditor.site)

    private class KotlinAnnotationBrowserInformationControlInput(
        val element: KtElement,
        previousInput: BrowserInformationControlInput? = null
    ) : BrowserInformationControlInput(previousInput) {

        companion object {
            const val imageWidth = 16
            const val imageHeight = 16
            const val labelLeft = 20
            const val labelTop = 2
            val javadocStyleSheet: String by lazy { loadStyleSheet("/JavadocHoverStyleSheet.css") }
            val registry: ColorRegistry by lazy { JFaceResources.getColorRegistry() }
            val fgRGB: RGB by lazy { registry.getRGB("org.eclipse.jdt.ui.Javadoc.foregroundColor") }
            val bgRGB: RGB by lazy { registry.getRGB("org.eclipse.jdt.ui.Javadoc.backgroundColor") }

            fun wrappedHeader(content: String) =
                "<h5><div style='word-wrap: break-word; position: relative; margin-left: ${labelLeft}px; padding-top: ${labelTop}px;'>$content</div></h5>"

            fun img(src: URL) =
                "<img style='border:none; position: absolute; width: ${imageWidth}px; height: ${imageHeight}px; left: -${labelLeft + 1}px;' src='$src'/>"
        }

        private val expression = getReferenceExpression(element)

        private val descriptor = expression?.let {
            KotlinAnalysisFileCache.getAnalysisResult(element.containingKtFile).analysisResult.bindingContext[REFERENCE_TARGET, expression]
        } ?: (element as? KtDeclaration)?.resolveToDescriptor()

        override fun getHtml(): String? =
            descriptor?.header?.let { header ->
                val builder = StringBuilder()
                HTMLPrinter.insertPageProlog(builder, 0, fgRGB, bgRGB, javadocStyleSheet)
                builder.append(wrappedHeader(header))
                val content = (descriptor.findKDoc() ?: getKtFileFromElement(element, descriptor)
                    ?.findDeclarationForDescriptor(descriptor)?.docComment?.getDefaultSection())?.getContent()
                    ?: element.getJavadocIfAvailable()
                content?.let {
                    builder.append("<body><br><p>$it</p></body>")
                }
                HTMLPrinter.addPageEpilog(builder)
                builder.toString()
            }

        override fun getInputElement(): Any? = element

        override fun getInputName(): String = element.name.toString()

        private fun KtElement.getJavadocIfAvailable(): String? =
            (resolveToSourceDeclaration().firstOrNull() as? EclipseJavaSourceElement)?.elementBinding?.javaElement?.let {
                JavadocContentAccess2.getHTMLContent(it, true)
            }

        private fun getImage(value: DeclarationDescriptorWithVisibility) =
            JavaPlugin.getDefault().imagesOnFSRegistry.getImageURL(value.getImage())

        private fun KtFile.findDeclarationForDescriptor(descriptor: DeclarationDescriptor): KtDeclaration? {
            return findDeclarationInParsedFile(descriptor, this)
        }

        private val DeclarationDescriptor.header: String?
            get() = when (this) {
                is ValueDescriptor ->
                    "${img(getImage(this))} ${if (isExtensionProperty) "${extensionReceiverParameter?.type}." else ""}$name: $type ${definedIn()}"
                is FunctionDescriptor ->
                    "${img(getImage(this))} fun ${if (isExtension) "${extensionReceiverParameter?.type}." else ""}$name: (" +
                            valueParameters.joinToString(separator = ", ") { param -> "${param.name}: ${param.type}" } +
                            ") -> $returnType ${definedIn()}"
                is ClassDescriptor ->
                    "${img(getImage(this))}  $kindString $name ${definedIn()}"
                else -> null
            }

        private fun DeclarationDescriptorWithVisibility.getImage() =
            when {
                this is ValueDescriptor -> when (visibility.name) {
                    "local" -> JavaPluginImages.DESC_OBJS_LOCAL_VARIABLE
                    "private" -> JavaPluginImages.DESC_FIELD_PRIVATE
                    "public" -> JavaPluginImages.DESC_FIELD_PUBLIC
                    else -> JavaPluginImages.DESC_FIELD_DEFAULT
                }
                this is ClassDescriptor -> when (kind) {
                    INTERFACE -> JavaPluginImages.DESC_OBJS_INTERFACE
                    ENUM_CLASS, ENUM_ENTRY -> JavaPluginImages.DESC_OBJS_ENUM
                    else -> JavaPluginImages.DESC_OBJS_CLASS
                }
                else -> when (visibility.name) {
                    "local" -> JavaPluginImages.DESC_OBJS_LOCAL_VARIABLE
                    "private" -> JavaPluginImages.DESC_MISC_PRIVATE
                    "public" -> JavaPluginImages.DESC_MISC_PUBLIC
                    else -> JavaPluginImages.DESC_MISC_DEFAULT
                }
            }

        private fun DeclarationDescriptor.definedIn() = with(containingDeclaration) {
            "defined in " + when (this) {
                is FunctionDescriptor -> "fun $fqNameSafe"
                is LazyJavaPackageFragment -> "package $fqName"
                is LazyClassDescriptor -> "$kindString $name"
                else -> this
            }
        }

        private val ClassDescriptor.kindString: String
            get() = when (kind) {
                CLASS -> "class"
                INTERFACE -> "interface"
                ENUM_CLASS -> "enum class"
                ENUM_ENTRY -> "enum entry"
                ANNOTATION_CLASS -> "annotation class"
                OBJECT -> "object"
                else -> ""
            }
    }
}
