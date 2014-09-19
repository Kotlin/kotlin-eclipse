package org.jetbrains.kotlin.ui.launch.junit;

import java.util.List;
import java.util.Set;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.junit.launcher.ITestKind;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.model.KotlinJavaManager;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class KotlinJUnitLaunchableTester extends PropertyTester {

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (receiver instanceof IFile) {
            IFile file = (IFile) receiver;
            JetFile jetFile = (JetFile) KotlinPsiManager.INSTANCE.getParsedFile(file);
            List<JetClass> jetClasses = getJetClasses(jetFile);
            if (jetClasses.size() == 1) {
                IType type = KotlinJavaManager.INSTANCE.findEclipseType(jetClasses.get(0), JavaCore.create(file.getProject()));
                return type != null ? checkHasTests(type) : false;
            }
        }
        
        return false;
    }
    
    private boolean checkHasTests(@NotNull IJavaElement element) {
        try {
            ITestKind testKind = TestKindRegistry.getDefault().getKind(TestKindRegistry.getContainerTestKindId(element));
            
            Set<IType> tests = Sets.newHashSet();
            testKind.getFinder().findTestsInContainer(element, tests, null);
            
            return !tests.isEmpty();
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return false;
    }
    
    private static List<JetClass> getJetClasses(@NotNull JetFile jetFile) {
        List<JetClass> jetClasses = Lists.newArrayList();
        for (JetDeclaration declaration : jetFile.getDeclarations()) {
            if (declaration instanceof JetClass) {
                jetClasses.add((JetClass) declaration);
            }
        }
        
        return jetClasses;
    }
}
