package org.jetbrains.kotlin.checkers;

import static org.jetbrains.kotlin.diagnostics.Errors.ASSIGN_OPERATOR_AMBIGUITY;
import static org.jetbrains.kotlin.diagnostics.Errors.CANNOT_COMPLETE_RESOLVE;
import static org.jetbrains.kotlin.diagnostics.Errors.COMPONENT_FUNCTION_AMBIGUITY;
import static org.jetbrains.kotlin.diagnostics.Errors.DELEGATE_SPECIAL_FUNCTION_AMBIGUITY;
import static org.jetbrains.kotlin.diagnostics.Errors.DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE;
import static org.jetbrains.kotlin.diagnostics.Errors.ITERATOR_AMBIGUITY;
import static org.jetbrains.kotlin.diagnostics.Errors.NONE_APPLICABLE;
import static org.jetbrains.kotlin.diagnostics.Errors.OVERLOAD_RESOLUTION_AMBIGUITY;
import static org.jetbrains.kotlin.diagnostics.Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER;

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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.asJava.DuplicateJvmSignatureUtilKt;
import org.jetbrains.kotlin.checkers.CheckerTestUtil.ActualDiagnostic;
import org.jetbrains.kotlin.checkers.CheckerTestUtil.TextDiagnostic;
import org.jetbrains.kotlin.checkers.KotlinDiagnosticsTestCase.TestFile;
import org.jetbrains.kotlin.checkers.KotlinDiagnosticsTestCase.TestModule;
import org.jetbrains.kotlin.core.model.KotlinEnvironment;
import org.jetbrains.kotlin.core.resolve.EclipseAnalyzerFacadeForJVM;
import org.jetbrains.kotlin.core.tests.diagnostics.AdditionalConditions;
import org.jetbrains.kotlin.core.tests.diagnostics.JetLightFixture;
import org.jetbrains.kotlin.core.tests.diagnostics.JetTestUtils;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory2;
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.diagnostics.Severity;
import org.jetbrains.kotlin.psi.Call;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.AnalyzingUtils;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.MultiTargetPlatform;
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics;
import org.jetbrains.kotlin.testframework.editor.KotlinProjectTestCase;
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils;
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
import com.intellij.util.containers.ContainerUtil;

import junit.framework.TestCase;
import kotlin.Pair;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;

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
    public static final String CHECK_TYPE_PACKAGE = "tests._checkType";
    private static final String CHECK_TYPE_DECLARATIONS = "\npackage " + CHECK_TYPE_PACKAGE +
            "\nfun <T> checkSubtype(t: T) = t" +
            "\nclass Inv<T>" +
            "\nfun <E> Inv<E>._() {}" +
            "\ninfix fun <T> T.checkType(f: Inv<T>.() -> Unit) {}";
    
    public static final String CHECK_TYPE_IMPORT = "import " + CHECK_TYPE_PACKAGE + ".*";
    
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
            List<TestModule> dependencies = CollectionsKt.map(
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
        Map<TestModule, List<TestFile>> groupedByModule = CollectionsKt.groupByTo(
                testFiles,
                new LinkedHashMap<TestModule, List<TestFile>>(),
                new Function1<TestFile, TestModule>() {
                    @Override
                    public TestModule invoke(TestFile file) {
                        return file.getModule();
                    }
                }
        );
        
        List<KtFile> allKtFiles = new ArrayList<KtFile>();
        Map<TestModule, BindingContext> moduleBindings = new HashMap<TestModule, BindingContext>();

        for (Map.Entry<TestModule, List<TestFile>> entry : groupedByModule.entrySet()) {
            TestModule testModule = entry.getKey();
            List<? extends TestFile> testFilesInModule = entry.getValue();

            List<KtFile> jetFiles = getKtFiles(testFilesInModule, true);
            allKtFiles.addAll(jetFiles);
            
            AnalysisResult analysisResult = EclipseAnalyzerFacadeForJVM.INSTANCE
                    .analyzeFilesWithJavaIntegration(
                            KotlinEnvironment.getEnvironment(getTestProject().getJavaProject().getProject()), jetFiles)
                    .getAnalysisResult();
            
            moduleBindings.put(testModule, analysisResult.getBindingContext());
            
            checkAllResolvedCallsAreCompleted(jetFiles, analysisResult.getBindingContext());
        }

        boolean ok = true;

        StringBuilder actualText = new StringBuilder();
        for (TestFile testFile : testFiles) {
            ok &= testFile.getActualText(moduleBindings.get(testFile.getModule()), actualText, true);
        }

        JetTestUtils.assertEqualsToFile(testDataFile, actualText.toString());

        TestCase.assertTrue("Diagnostics mismatch. See the output above", ok);
    }
    
    private static void checkAllResolvedCallsAreCompleted(@NotNull List<KtFile> jetFiles, @NotNull BindingContext bindingContext) {
        for (KtFile file : jetFiles) {
            if (!AnalyzingUtils.getSyntaxErrorRanges(file).isEmpty()) {
                return;
            }
        }

        ImmutableMap<Call,ResolvedCall<?>> resolvedCallsEntries = bindingContext.getSliceContents(BindingContext.RESOLVED_CALL);
        for (Entry<Call, ResolvedCall<?>> entry : resolvedCallsEntries.entrySet()) {
            KtElement element = entry.getKey().getCallElement();
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
        Set<DiagnosticFactory2<KtExpression,? extends Comparable<?>,Collection<? extends ResolvedCall<?>>>>
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
    
    protected List<KtFile> getKtFiles(List<? extends TestFile> testFiles, boolean includeExtras) {
        boolean declareCheckType = false;
        List<KtFile> jetFiles = Lists.newArrayList();
        for (TestFile testFile : testFiles) {
            if (testFile.getKtFile() != null) {
                jetFiles.add(testFile.getKtFile());
            }
            declareCheckType |= testFile.declareCheckType;
        }
        
        if (includeExtras) {
            if (declareCheckType) {
                jetFiles.add(JetLightFixture.createPsiFile(null, "CHECK_TYPE.kt", CHECK_TYPE_DECLARATIONS, getProject()));
            }
        }


        return jetFiles;
    }
    
    private boolean writeJavaFile(@NotNull String filePath, @NotNull String content) {
        try {
            File sourceFile = new File(filePath);
            getTestProject().createSourceFile(
                    sourceFile.getParent(), 
                    sourceFile.getName(), 
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
        private final KtFile jetFile;
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
                this.expectedText = textWithMarkers;
                String textWithExtras = addExtras(expectedText);
                this.clearText = CheckerTestUtil.parseDiagnosedRanges(textWithExtras, diagnosedRanges);
                this.jetFile = JetLightFixture.createCheckAndReturnPsiFile(null, fileName, clearText, getProject());
            }
        }
        
        private String addExtras(String text) {
            return addImports(text, getExtras());
        }
        
        private String getExtras() {
            return "/*extras*/\n" + getImports() + "/*extras*/\n\n";
        }
        
        @NotNull
        private String getImports() {
            String imports = "";
            if (declareCheckType) {
                imports += CHECK_TYPE_IMPORT + "\n";
            }
            return imports;
        }
        
        private String addImports(String text, String imports) {
            Pattern pattern = Pattern.compile("^package [\\.\\w\\d]*\n", Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                // add imports after the package directive
                text = text.substring(0, matcher.end()) + imports + text.substring(matcher.end());
            }
            else {
                // add imports at the beginning
                text = imports + text;
            }
            return text;
        }

        public TestModule getModule() {
            return module;
        }

        public KtFile getKtFile() {
            return jetFile;
        }
        
        private void stripExtras(StringBuilder actualText) {
            String extras = getExtras();
            int start = actualText.indexOf(extras);
            if (start >= 0) {
                actualText.delete(start, start + extras.length());
            }
        }
        
        public boolean getActualText(BindingContext bindingContext, StringBuilder actualText, boolean skipJvmSignatureDiagnostics) {
            if (this.jetFile == null) {
                // TODO: check java files too
                actualText.append(this.clearText);
                return true;
            }

            Set<ActualDiagnostic> jvmSignatureDiagnostics = skipJvmSignatureDiagnostics
                                                            ? Collections.<ActualDiagnostic>emptySet()
                                                            : computeJvmSignatureDiagnostics(bindingContext);

            final boolean[] ok = { true };
            List<Pair<MultiTargetPlatform, BindingContext>> implementingModulesBinding = Lists.newArrayList();
            List<ActualDiagnostic> diagnostics = ContainerUtil.filter(
                    CollectionsKt.plus(
                    		CheckerTestUtil.getDiagnosticsIncludingSyntaxErrors(
                    		        bindingContext, 
                    		        implementingModulesBinding,
                    		        jetFile, 
                    		        markDynamicCalls, 
                    		        dynamicCallDescriptors), 
                    		jvmSignatureDiagnostics),
                    new Condition<ActualDiagnostic>() {
                        @Override
                        public boolean value(final ActualDiagnostic actualDiagnostic) {
                            return whatDiagnosticsToConsider.value(actualDiagnostic.diagnostic);
                        }
                    });
            
            Map<ActualDiagnostic, CheckerTestUtil.TextDiagnostic> diagnosticToExpectedDiagnostic = CheckerTestUtil.diagnosticsDiff(
                    diagnosedRanges, diagnostics, new CheckerTestUtil.DiagnosticDiffCallbacks() {
                @Override
                public void missingDiagnostic(TextDiagnostic diagnostic, int expectedStart, int expectedEnd) {
                    String message = "Missing " + diagnostic.getDescription() + DiagnosticUtils.atLocation(jetFile, new TextRange(expectedStart, expectedEnd));
                    System.err.println(message);
                    ok[0] = false;
                }

                @Override
                public void unexpectedDiagnostic(TextDiagnostic diagnostic, int actualStart, int actualEnd) {
                    String message = "Unexpected " + diagnostic.getDescription() + DiagnosticUtils.atLocation(jetFile, new TextRange(actualStart, actualEnd));
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
            
            stripExtras(actualText);
            
            return ok[0];
        }

        private Set<ActualDiagnostic> computeJvmSignatureDiagnostics(BindingContext bindingContext) {
            Set<ActualDiagnostic> jvmSignatureDiagnostics = new HashSet<ActualDiagnostic>();
            Collection<KtDeclaration> declarations = PsiTreeUtil.findChildrenOfType(jetFile, KtDeclaration.class);
            for (KtDeclaration declaration : declarations) {
                Diagnostics diagnostics = DuplicateJvmSignatureUtilKt.getJvmSignatureDiagnostics(declaration, 
                        bindingContext.getDiagnostics(), GlobalSearchScope.allScope(getProject()));
                if (diagnostics == null) continue;
                jvmSignatureDiagnostics.addAll(CollectionsKt.map(diagnostics.forElement(declaration), new Function1<Diagnostic, ActualDiagnostic>() {
                    @Override
                    public ActualDiagnostic invoke(Diagnostic arg0) {
                        return new ActualDiagnostic(arg0, null);
                    }
                }));
                  
            }
            return jvmSignatureDiagnostics;
        }
    }
}