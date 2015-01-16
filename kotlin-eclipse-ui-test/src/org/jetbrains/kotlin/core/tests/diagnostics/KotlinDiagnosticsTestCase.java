package org.jetbrains.kotlin.core.tests.diagnostics;

import static org.jetbrains.jet.lang.diagnostics.Errors.ASSIGN_OPERATOR_AMBIGUITY;
import static org.jetbrains.jet.lang.diagnostics.Errors.CANNOT_COMPLETE_RESOLVE;
import static org.jetbrains.jet.lang.diagnostics.Errors.COMPONENT_FUNCTION_AMBIGUITY;
import static org.jetbrains.jet.lang.diagnostics.Errors.DELEGATE_SPECIAL_FUNCTION_AMBIGUITY;
import static org.jetbrains.jet.lang.diagnostics.Errors.DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE;
import static org.jetbrains.jet.lang.diagnostics.Errors.ITERATOR_AMBIGUITY;
import static org.jetbrains.jet.lang.diagnostics.Errors.NONE_APPLICABLE;
import static org.jetbrains.jet.lang.diagnostics.Errors.OVERLOAD_RESOLUTION_AMBIGUITY;
import static org.jetbrains.jet.lang.diagnostics.Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;
import kotlin.Function1;
import kotlin.KotlinPackage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalysisResult;
import org.jetbrains.jet.asJava.AsJavaPackage;
import org.jetbrains.jet.checkers.CheckerTestUtil;
import org.jetbrains.jet.checkers.CheckerTestUtil.TextDiagnostic;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory1;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory2;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.model.MutableResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.diagnostics.Diagnostics;
import org.jetbrains.jet.lang.resolve.java.TopDownAnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.kotlin.core.resolve.EclipseAnalyzerFacadeForJVM;
import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase;
import org.junit.Assert;
import org.junit.Before;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;

public class KotlinDiagnosticsTestCase extends KotlinProjectTestCase {
    
    public static final Pattern DIAGNOSTICS_PATTERN = Pattern.compile("([\\+\\-!])(\\w+)\\s*");
    public static final String DIAGNOSTICS_DIRECTIVE = "DIAGNOSTICS";
    @SuppressWarnings("unchecked")
    public static final ImmutableSet<DiagnosticFactory<?>> DIAGNOSTICS_TO_INCLUDE_ANYWAY =
        ImmutableSet.of(
                Errors.UNRESOLVED_REFERENCE,
                Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER,
                CheckerTestUtil.SyntaxErrorDiagnosticFactory.INSTANCE,
                CheckerTestUtil.DebugInfoDiagnosticFactory.ELEMENT_WITH_ERROR_TYPE,
                CheckerTestUtil.DebugInfoDiagnosticFactory.MISSING_UNRESOLVED,
                CheckerTestUtil.DebugInfoDiagnosticFactory.UNRESOLVED_WITH_TARGET
        );
    public static final String CHECK_TYPE_DIRECTIVE = "CHECK_TYPE";
    private static final String CHECK_TYPE_DECLARATIONS = "\nclass _<T>" +
            "\nfun <T> T.checkType(f: (_<T>) -> Unit) = f";
    
    public static final String MARK_DYNAMIC_CALLS_DIRECTIVE = "MARK_DYNAMIC_CALLS";
    
    @Before
    public void configure() {
        configureProject();
    }

    protected void doTest(String filePath) throws IOException {
        File file = new File(filePath);
        
        String expectedText = JetTestUtils.doLoadFile(file);
        expectedText = StringUtilRt.convertLineSeparators(expectedText);

        class ModuleAndDependencies {
            final TestModule module;
            final List<String> dependencies;

            ModuleAndDependencies(TestModule module, List<String> dependencies) {
                this.module = module;
                this.dependencies = dependencies;
            }
        }
        final Map<String, ModuleAndDependencies> modules = new HashMap<String, ModuleAndDependencies>();

        List<TestFile> testFiles =
                JetTestUtils.createTestFiles(file.getName(), expectedText, new JetTestUtils.TestFileFactory<TestModule, TestFile>() {

                    @Override
                    public TestFile createFile(@Nullable TestModule module, String fileName, String text, Map<String, String> directives) {
                        if (fileName.endsWith(".java")) {
                            writeJavaFile(fileName, text);
                        }
                        
                        return new TestFile(module, fileName, text, directives);
                    }

                    @Override
                    public TestModule createModule(String name, List<String> dependencies) {
                        TestModule module = new TestModule(name);
                        ModuleAndDependencies oldValue = modules.put(name, new ModuleAndDependencies(module, dependencies));
                        assert oldValue == null : "Module " + name + " declared more than once";

                        return module;
                    }
                });

        for (final ModuleAndDependencies moduleAndDependencies : modules.values()) {
            List<TestModule> dependencies = KotlinPackage.map(
                    moduleAndDependencies.dependencies,
                    new Function1<String, TestModule>() {
                        @Override
                        public TestModule invoke(String name) {
                            ModuleAndDependencies dependency = modules.get(name);
                            assert dependency != null : "Dependency not found: " + name + " for module " + moduleAndDependencies.module.getName();
                            return dependency.module;
                        }
                    }
            );
            moduleAndDependencies.module.getDependencies().addAll(dependencies);
        }

        analyzeAndCheck(file, testFiles, "<" + file.getName().substring(0, file.getName().length() - ".kt".length()));
    }
    
    protected void analyzeAndCheck(File testDataFile, List<TestFile> testFiles, String moduleName) {
        Map<TestModule, List<TestFile>> groupedByModule = KotlinPackage.groupByTo(
                testFiles,
                new LinkedHashMap<TestModule, List<TestFile>>(),
                new Function1<TestFile, TestModule>() {
                    @Override
                    public TestModule invoke(TestFile file) {
                        return file.getModule();
                    }
                }
        );
        
        List<JetFile> allJetFiles = new ArrayList<JetFile>();
        Map<TestModule, ModuleDescriptorImpl> modules = createModules(groupedByModule);
        Map<TestModule, BindingContext> moduleBindings = new HashMap<TestModule, BindingContext>();

        for (Map.Entry<TestModule, List<TestFile>> entry : groupedByModule.entrySet()) {
            TestModule testModule = entry.getKey();
            List<? extends TestFile> testFilesInModule = entry.getValue();

            List<JetFile> jetFiles = getJetFiles(testFilesInModule);
            allJetFiles.addAll(jetFiles);

            ModuleDescriptorImpl module = modules.get(testModule);
            
            AnalysisResult analysisResult = EclipseAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                    getTestProject().getJavaProject(), getProject(),
                    jetFiles,
                    module
            );
            
            moduleBindings.put(testModule, analysisResult.getBindingContext());
            
            checkAllResolvedCallsAreCompleted(jetFiles, analysisResult.getBindingContext());
        }

        boolean ok = true;

        StringBuilder actualText = new StringBuilder();
        for (TestFile testFile : testFiles) {
            ok &= testFile.getActualText(moduleBindings.get(testFile.getModule()), actualText, groupedByModule.size() > 1);
        }

        JetTestUtils.assertEqualsToFile(testDataFile, actualText.toString());

        TestCase.assertTrue("Diagnostics mismatch. See the output above", ok);
    }
    
    private static void checkAllResolvedCallsAreCompleted(@NotNull List<JetFile> jetFiles, @NotNull BindingContext bindingContext) {
        for (JetFile file : jetFiles) {
            if (!AnalyzingUtils.getSyntaxErrorRanges(file).isEmpty()) {
                return;
            }
        }

        ImmutableMap<Call,ResolvedCall<?>> resolvedCallsEntries = bindingContext.getSliceContents(BindingContext.RESOLVED_CALL);
        for (Entry<Call, ResolvedCall<?>> entry : resolvedCallsEntries.entrySet()) {
            JetElement element = entry.getKey().getCallElement();
            ResolvedCall<?> resolvedCall = entry.getValue();

            DiagnosticUtils.LineAndColumn lineAndColumn =
                    DiagnosticUtils.getLineAndColumnInPsiFile(element.getContainingFile(), element.getTextRange());

            TestCase.assertTrue("Resolved call for '" + element.getText() + "'" + lineAndColumn + " is not completed",
                       ((MutableResolvedCall<?>) resolvedCall).isCompleted());
        }

        checkResolvedCallsInDiagnostics(bindingContext);
    }
    
    @SuppressWarnings({"unchecked"})
    private static void checkResolvedCallsInDiagnostics(BindingContext bindingContext) {
        Set<DiagnosticFactory1<PsiElement, Collection<? extends ResolvedCall<?>>>> diagnosticsStoringResolvedCalls1 = Sets.newHashSet(
                OVERLOAD_RESOLUTION_AMBIGUITY, NONE_APPLICABLE, CANNOT_COMPLETE_RESOLVE, UNRESOLVED_REFERENCE_WRONG_RECEIVER,
                ASSIGN_OPERATOR_AMBIGUITY, ITERATOR_AMBIGUITY);
        Set<DiagnosticFactory2<JetExpression,? extends Comparable<?>,Collection<? extends ResolvedCall<?>>>>
                diagnosticsStoringResolvedCalls2 = Sets.newHashSet(
                COMPONENT_FUNCTION_AMBIGUITY, DELEGATE_SPECIAL_FUNCTION_AMBIGUITY, DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE);
        
        Diagnostics diagnostics = bindingContext.getDiagnostics();
        for (Diagnostic diagnostic : diagnostics) {
            DiagnosticFactory<?> factory = diagnostic.getFactory();
            //noinspection SuspiciousMethodCalls
            if (diagnosticsStoringResolvedCalls1.contains(factory)) {
                assertResolvedCallsAreCompleted(
                        diagnostic, DiagnosticFactory.cast(diagnostic, diagnosticsStoringResolvedCalls1).getA());

            }
            //noinspection SuspiciousMethodCalls
            if (diagnosticsStoringResolvedCalls2.contains(factory)) {
                assertResolvedCallsAreCompleted(
                        diagnostic,
                        DiagnosticFactory.cast(diagnostic, diagnosticsStoringResolvedCalls2).getB());
            }
        }
    }
    
    public static Condition<Diagnostic> parseDiagnosticFilterDirective(Map<String, String> directiveMap) {
        String directives = directiveMap.get(DIAGNOSTICS_DIRECTIVE);
        if (directives == null) {
            return AdditionalConditions.alwaysTrue();
        }
        Condition<Diagnostic> condition = AdditionalConditions.alwaysTrue();
        Matcher matcher = DIAGNOSTICS_PATTERN.matcher(directives);
        if (!matcher.find()) {
            Assert.fail("Wrong syntax in the '// !DIAGNOSTICS: ...' directive:\n" +
                        "found: '" + directives + "'\n" +
                        "Must be '([+-!]DIAGNOSTIC_FACTORY_NAME|ERROR|WARNING|INFO)+'\n" +
                        "where '+' means 'include'\n" +
                        "      '-' means 'exclude'\n" +
                        "      '!' means 'exclude everything but this'\n" +
                        "directives are applied in the order of appearance, i.e. !FOO +BAR means include only FOO and BAR");
        }
        boolean first = true;
        do {
            String operation = matcher.group(1);
            final String name = matcher.group(2);

            Condition<Diagnostic> newCondition;
            if (ImmutableSet.of("ERROR", "WARNING", "INFO").contains(name)) {
                final Severity severity = Severity.valueOf(name);
                newCondition = new Condition<Diagnostic>() {
                    @Override
                    public boolean value(Diagnostic diagnostic) {
                        return diagnostic.getSeverity() == severity;
                    }
                };
            }
            else {
                newCondition = new Condition<Diagnostic>() {
                    @Override
                    public boolean value(Diagnostic diagnostic) {
                        return name.equals(diagnostic.getFactory().getName());
                    }
                };
            }
            if ("!".equals(operation)) {
                if (!first) {
                    Assert.fail("'" + operation + name + "' appears in a position rather than the first one, " +
                                "which effectively cancels all the previous filters in this directive");
                }
                condition = newCondition;
            }
            else if ("+".equals(operation)) {
                condition = AdditionalConditions.or(condition, newCondition);
            }
            else if ("-".equals(operation)) {
                condition = AdditionalConditions.and(condition, AdditionalConditions.not(newCondition));
            }
            first = false;
        }
        while (matcher.find());
        // We always include UNRESOLVED_REFERENCE and SYNTAX_ERROR because they are too likely to indicate erroneous test data
        return AdditionalConditions.or(
                condition,
                new Condition<Diagnostic>() {
                    @Override
                    public boolean value(Diagnostic diagnostic) {
                        return DIAGNOSTICS_TO_INCLUDE_ANYWAY.contains(diagnostic.getFactory());
                    }
                });
    }
    
    private static void assertResolvedCallsAreCompleted(
            @NotNull Diagnostic diagnostic, @NotNull Collection<? extends ResolvedCall<?>> resolvedCalls
    ) {
        boolean allCallsAreCompleted = true;
        for (ResolvedCall<?> resolvedCall : resolvedCalls) {
            if (!((MutableResolvedCall<?>) resolvedCall).isCompleted()) {
                allCallsAreCompleted = false;
            }
        }

        PsiElement element = diagnostic.getPsiElement();
        DiagnosticUtils.LineAndColumn lineAndColumn =
                DiagnosticUtils.getLineAndColumnInPsiFile(element.getContainingFile(), element.getTextRange());

        TestCase.assertTrue("Resolved calls stored in " + diagnostic.getFactory().getName() + "\n" +
                   "for '" + element.getText() + "'" + lineAndColumn + " are not completed",
                   allCallsAreCompleted);
    }
    
    private Map<TestModule, ModuleDescriptorImpl> createModules(Map<TestModule, List<TestFile>> groupedByModule) {
    	Map<TestModule, ModuleDescriptorImpl> modules = new HashMap<TestModule, ModuleDescriptorImpl>();

        for (TestModule testModule : groupedByModule.keySet()) {
            ModuleDescriptorImpl module =
                    testModule == null ?
                    TopDownAnalyzerFacadeForJVM.createSealedJavaModule() :
                    TopDownAnalyzerFacadeForJVM.createJavaModule("<" + testModule.getName() + ">");

            modules.put(testModule, module);
        }

        for (TestModule testModule : groupedByModule.keySet()) {
            if (testModule == null) continue;

            ModuleDescriptorImpl module = modules.get(testModule);
            module.addDependencyOnModule(module);
            for (TestModule dependency : testModule.getDependencies()) {
                module.addDependencyOnModule(modules.get(dependency));
            }

            module.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule());
            module.seal();
        }

        return modules;
    }
    
    protected static List<JetFile> getJetFiles(List<? extends TestFile> testFiles) {
        List<JetFile> jetFiles = Lists.newArrayList();
        for (TestFile testFile : testFiles) {
            if (testFile.getJetFile() != null) {
                jetFiles.add(testFile.getJetFile());
            }
        }
        return jetFiles;
    }
    
    private boolean writeJavaFile(@NotNull String filePath, @NotNull String content) {
        try {
            getTestProject().createSourceFile(
                    PathUtil.getParentPath(filePath), 
                    PathUtil.getFileName(filePath), 
                    content);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    protected static class TestModule {
        private final String name;
        private final List<TestModule> dependencies = new ArrayList<TestModule>();

        public TestModule(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public List<TestModule> getDependencies() {
            return dependencies;
        }
    }
    
    @NotNull
    public Project getProject() {
        return getTestProject().getKotlinEnvironment().getProject();
    }
    
    protected class TestFile {
        private final List<CheckerTestUtil.DiagnosedRange> diagnosedRanges = Lists.newArrayList();
        private final String expectedText;
        private final TestModule module;
        private final String clearText;
        private final JetFile jetFile;
        private final Condition<Diagnostic> whatDiagnosticsToConsider;
        private final boolean markDynamicCalls;
        private final boolean declareCheckType;
        private final List<DeclarationDescriptor> dynamicCallDescriptors = new ArrayList<DeclarationDescriptor>();

        public TestFile(
                TestModule module,
                String fileName,
                String textWithMarkers,
                Map<String, String> directives
        ) {
            this.module = module;
            this.whatDiagnosticsToConsider = parseDiagnosticFilterDirective(directives);
            this.declareCheckType = directives.containsKey(CHECK_TYPE_DIRECTIVE);
            this.markDynamicCalls = directives.containsKey(MARK_DYNAMIC_CALLS_DIRECTIVE);
            if (fileName.endsWith(".java")) {
                PsiFileFactory.getInstance(getProject()).createFileFromText(fileName, JavaLanguage.INSTANCE, textWithMarkers);
                // TODO: check there's not syntax errors
                this.jetFile = null;
                this.expectedText = this.clearText = textWithMarkers;
            }
            else {
                expectedText = textWithMarkers;
                clearText = CheckerTestUtil.parseDiagnosedRanges(expectedText, diagnosedRanges);
                this.jetFile = JetLightFixture.createCheckAndReturnPsiFile(
                        null, fileName, declareCheckType ? clearText + CHECK_TYPE_DECLARATIONS : clearText, getProject());
                for (CheckerTestUtil.DiagnosedRange diagnosedRange : diagnosedRanges) {
                    diagnosedRange.setFile(jetFile);
                }
            }
        }

        public TestModule getModule() {
            return module;
        }

        public JetFile getJetFile() {
            return jetFile;
        }
        
        public boolean getActualText(BindingContext bindingContext, StringBuilder actualText, boolean skipJvmSignatureDiagnostics) {
            if (this.jetFile == null) {
                // TODO: check java files too
                actualText.append(this.clearText);
                return true;
            }

            Set<Diagnostic> jvmSignatureDiagnostics = skipJvmSignatureDiagnostics
                                                            ? Collections.<Diagnostic>emptySet()
                                                            : computeJvmSignatureDiagnostics(bindingContext);

            final boolean[] ok = { true };
            List<Diagnostic> diagnostics = ContainerUtil.filter(
                    KotlinPackage.plus(
                    		CheckerTestUtil.getDiagnosticsIncludingSyntaxErrors(bindingContext, jetFile, markDynamicCalls, dynamicCallDescriptors), jvmSignatureDiagnostics),
                    whatDiagnosticsToConsider
            );
            
            Map<Diagnostic, CheckerTestUtil.TextDiagnostic> diagnosticToExpectedDiagnostic = ContainerUtil.newHashMap();
            CheckerTestUtil.diagnosticsDiff(diagnosticToExpectedDiagnostic, diagnosedRanges, diagnostics, new CheckerTestUtil.DiagnosticDiffCallbacks() {
                @Override
                public void missingDiagnostic(TextDiagnostic diagnostic, int expectedStart, int expectedEnd) {
                    String message = "Missing " + diagnostic.getName() + DiagnosticUtils.atLocation(jetFile, new TextRange(expectedStart, expectedEnd));
                    System.err.println(message);
                    ok[0] = false;
                }

                @Override
                public void unexpectedDiagnostic(TextDiagnostic diagnostic, int actualStart, int actualEnd) {
                    String message = "Unexpected " + diagnostic.getName() + DiagnosticUtils.atLocation(jetFile, new TextRange(actualStart, actualEnd));
                    System.err.println(message);
                    ok[0] = false;
                }

				@Override
				public void wrongParametersDiagnostic(
						TextDiagnostic expectedDiagnostic,
						TextDiagnostic actualDiagnostic, int start, int end) {
					String message = "Parameters of diagnostic not equal at position "
                                     + DiagnosticUtils.atLocation(jetFile, new TextRange(start, end))
                                     + ". Expected: " + expectedDiagnostic.asString() + ", actual: " + actualDiagnostic.asString();
		            System.err.println(message);
		            ok[0] = false;
				}
            });

            actualText.append(CheckerTestUtil.addDiagnosticMarkersToText(jetFile, diagnostics, diagnosticToExpectedDiagnostic, new Function<PsiFile, String>() {
                @Override
                public String fun(PsiFile file) {
                    String text = file.getText();
                    return declareCheckType ? StringUtil.trimEnd(text, CHECK_TYPE_DECLARATIONS) : text;
                }
            }));
            
            return ok[0];
        }

        private Set<Diagnostic> computeJvmSignatureDiagnostics(BindingContext bindingContext) {
            Set<Diagnostic> jvmSignatureDiagnostics = new HashSet<Diagnostic>();
            Collection<JetDeclaration> declarations = PsiTreeUtil.findChildrenOfType(jetFile, JetDeclaration.class);
            for (JetDeclaration declaration : declarations) {
                Diagnostics diagnostics = AsJavaPackage.getJvmSignatureDiagnostics(declaration, 
                        bindingContext.getDiagnostics(), GlobalSearchScope.allScope(getProject()));
                if (diagnostics == null) continue;
                jvmSignatureDiagnostics.addAll(diagnostics.forElement(declaration));
            }
            return jvmSignatureDiagnostics;
        }
    }
}