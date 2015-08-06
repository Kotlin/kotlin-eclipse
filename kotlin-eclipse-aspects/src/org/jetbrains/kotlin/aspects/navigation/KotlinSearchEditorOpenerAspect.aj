package org.jetbrains.kotlin.aspects.navigation;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.search.JavaSearchEditorOpener;
import org.eclipse.ui.IEditorPart;
import org.jetbrains.kotlin.core.resolve.lang.java.structure.EclipseJavaElementUtil;
import org.jetbrains.kotlin.ui.navigation.KotlinOpenEditor;

public aspect KotlinSearchEditorOpenerAspect {
    pointcut openElement(Object element) :
        args(element)
        && execution(IEditorPart JavaSearchEditorOpener.openElement(Object));
    
    @SuppressAjWarnings({"adviceDidNotMatch"})
    IEditorPart around(Object element) : openElement(element) {
        if (element instanceof IJavaElement) {
            IJavaElement javaElement = (IJavaElement) element;
            
            if (EclipseJavaElementUtil.isKotlinLightClass(javaElement)) {
                IEditorPart kotlinEditor = KotlinOpenEditor.openKotlinEditor(javaElement, true);
                EditorUtility.revealInEditor(kotlinEditor, javaElement);
                
                return null;
            }
        }
        
        return proceed(element);
    }
}
