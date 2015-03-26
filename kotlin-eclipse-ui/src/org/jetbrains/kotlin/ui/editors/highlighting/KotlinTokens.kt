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
import org.eclipse.jface.text.rules.Token
import org.jetbrains.kotlin.psi.JetPsiUtil
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens

class KotlinTokensFactory(val preferenceStore: IPreferenceStore, val colorManager: IColorManager) {
	val keywordToken = createToken(PreferenceConstants.EDITOR_JAVA_KEYWORD_COLOR)
	val identifierToken = createToken(PreferenceConstants.EDITOR_JAVA_DEFAULT_COLOR)
	val stringToken = createToken(PreferenceConstants.EDITOR_STRING_COLOR)
	val commentToken = createToken(PreferenceConstants.EDITOR_SINGLE_LINE_COMMENT_COLOR)
	val kdocTagNameToken = createToken(style = SWT.BOLD)
	val whitespaceToken = createToken()
	
	fun getToken(leafElement: PsiElement): IToken {
		if (leafElement !is LeafPsiElement) return Token.UNDEFINED
		
		val elementType = leafElement.getElementType()
		return when {
			elementType in JetTokens.KEYWORDS, 
				elementType in JetTokens.SOFT_KEYWORDS, 
				elementType in JetTokens.MODIFIER_KEYWORDS -> keywordToken
			
			JetTokens.IDENTIFIER.equals(elementType) -> identifierToken
			
			elementType in JetTokens.STRINGS,
				JetTokens.OPEN_QUOTE.equals(elementType),
				JetTokens.CLOSING_QUOTE.equals(elementType) -> stringToken
			
			elementType in JetTokens.WHITESPACES -> whitespaceToken
			
			elementType in JetTokens.COMMENTS,
				elementType in KDocTokens.KDOC_HIGHLIGHT_TOKENS -> commentToken
			
			KDocTokens.TAG_NAME.equals(elementType) -> kdocTagNameToken
			
			else -> Token.UNDEFINED
		}
	}
	
	private fun createToken(colorKey: String = PreferenceConstants.EDITOR_JAVA_DEFAULT_COLOR, style: Int = SWT.NORMAL): Token {
		val color = colorManager.getColor(PreferenceConverter.getColor(preferenceStore, colorKey))
		val attribute = TextAttribute(color, null, style)
		return Token(attribute)
	}
}