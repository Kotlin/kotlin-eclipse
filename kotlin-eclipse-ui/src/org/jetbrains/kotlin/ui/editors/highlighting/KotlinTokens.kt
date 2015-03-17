package org.jetbrains.kotlin.ui.editors.highlighting

import org.eclipse.jface.text.rules.IToken
import org.eclipse.jface.text.TextAttribute
import org.eclipse.jdt.ui.text.IColorManager
import org.eclipse.swt.SWT
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.JetTokens
import com.intellij.psi.PsiElement
import kotlin.platform.platformStatic
import org.eclipse.jface.preference.PreferenceConverter
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jdt.ui.PreferenceConstants

class KotlinTokenSettings(val preferenceStore: IPreferenceStore, val colorManager: IColorManager)

class KotlinTokenParameters(val colorKey: String = PreferenceConstants.EDITOR_JAVA_DEFAULT_COLOR,
		val style: Int = SWT.NORMAL,
		val isUndefined: Boolean = true,
		val isWhitespace: Boolean = false,
		val isEOF: Boolean = false,
		val isOther: Boolean = false)

val keywordParam = KotlinTokenParameters(PreferenceConstants.EDITOR_JAVA_KEYWORD_COLOR, isOther = true)
val identifierParam = KotlinTokenParameters(PreferenceConstants.EDITOR_JAVA_DEFAULT_COLOR, isOther = true)
val stringParam = KotlinTokenParameters(PreferenceConstants.EDITOR_STRING_COLOR, isOther = true)
val commentParam = KotlinTokenParameters(PreferenceConstants.EDITOR_SINGLE_LINE_COMMENT_COLOR, isOther = true)
val eofParam = KotlinTokenParameters(isEOF = true)
val whitespaceParam = KotlinTokenParameters(isWhitespace = true)
val undefinedParam = KotlinTokenParameters(isUndefined = true) 

private class KotlinToken(
		val settings: KotlinTokenSettings, 
		val parameters: KotlinTokenParameters) : IToken {
			
	override fun isUndefined(): Boolean = parameters.isUndefined
	
	override fun isWhitespace(): Boolean = parameters.isWhitespace
	
	override fun isEOF(): Boolean = parameters.isEOF
	
	override fun isOther(): Boolean = parameters.isOther
	
	override fun getData(): Any {
		val color = settings.colorManager.getColor(PreferenceConverter.getColor(settings.preferenceStore, parameters.colorKey))
		return TextAttribute(color, null, parameters.style)
	}
	
	companion object {
		platformStatic fun create(leafElement: PsiElement, settings: KotlinTokenSettings): KotlinToken {
			if (leafElement is LeafPsiElement) {
				val elementType = leafElement.getElementType()
				return when {
					JetTokens.KEYWORDS.contains(elementType), 
						JetTokens.SOFT_KEYWORDS.contains(elementType), 
						JetTokens.MODIFIER_KEYWORDS.contains(elementType) -> KotlinToken(settings, keywordParam)
					
					JetTokens.IDENTIFIER.equals(elementType) -> KotlinToken(settings, identifierParam)
					
					JetTokens.STRINGS.contains(elementType),
						JetTokens.OPEN_QUOTE.equals(elementType),
						JetTokens.CLOSING_QUOTE.equals(elementType) -> KotlinToken(settings, stringParam)
					
					JetTokens.WHITESPACES.contains(elementType) -> KotlinToken(settings, whitespaceParam)
					
					JetTokens.COMMENTS.contains(elementType) -> KotlinToken(settings, commentParam)
					
					else -> KotlinToken(settings, undefinedParam)
				}
			}
			
			return KotlinToken(settings, undefinedParam)
		}
	}
}

