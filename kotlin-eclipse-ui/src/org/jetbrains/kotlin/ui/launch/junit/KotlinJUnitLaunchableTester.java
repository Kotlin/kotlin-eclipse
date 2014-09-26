/*******************************************************************************
 * Copyright 2010-2014 JetBrains s.r.o.
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
 *******************************************************************************/
package org.jetbrains.kotlin.ui.launch.junit;

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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.model.KotlinJavaManager;

import com.google.common.collect.Sets;

public class KotlinJUnitLaunchableTester extends PropertyTester {

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (receiver instanceof IFile) {
            IType type = getEclipseTypeForSingleClass((IFile) receiver);
            return type != null ? checkHasTests(type) : false;
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
    
    @Nullable
    private static JetClass getSingleJetClass(@NotNull IFile file) {
        JetFile jetFile = (JetFile) KotlinPsiManager.INSTANCE.getParsedFile(file);
        
        JetClass jetClass = null;
        for (JetDeclaration declaration : jetFile.getDeclarations()) {
            if (declaration instanceof JetClass) {
                if (jetClass != null) {
                    return null;
                } else {
                    jetClass = (JetClass) declaration;
                }
            }
        }
        
        return jetClass;
    }
    
    @Nullable
    static IType getEclipseTypeForSingleClass(@NotNull IFile file) {
        JetClass jetClass = getSingleJetClass(file);
        return jetClass != null ? KotlinJavaManager.INSTANCE.findEclipseType(jetClass, JavaCore.create(file.getProject())) : null;
    }
}
