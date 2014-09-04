package org.jetbrains.kotlin.core.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AdditionalCheckerProvider;
import org.jetbrains.jet.lang.resolve.kotlin.JavaDeclarationCheckerProvider;
import org.jetbrains.jet.lang.resolve.lazy.ElementResolver;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;

public class KotlinLazyElementResolver extends ElementResolver {
    public KotlinLazyElementResolver(ResolveSession session) {
        super(session);
    }
    
    @Override   
    @NotNull
    protected AdditionalCheckerProvider getAdditionalCheckerProvider(@NotNull JetFile jetFile) {
        return JavaDeclarationCheckerProvider.INSTANCE$;
    }
}
