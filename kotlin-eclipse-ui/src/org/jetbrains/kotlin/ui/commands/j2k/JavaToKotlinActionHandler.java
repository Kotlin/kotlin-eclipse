package org.jetbrains.kotlin.ui.commands.j2k;

import static org.eclipse.ui.ide.undo.WorkspaceUndoUtil.getUIInfoAdapter;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import kotlin.Pair;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.DocumentAdapter;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.undo.CreateFileOperation;
import org.eclipse.ui.ide.undo.DeleteResourcesOperation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.model.KotlinEnvironment;
import org.jetbrains.kotlin.core.model.KotlinNature;
import org.jetbrains.kotlin.j2k.J2kPackage;
import org.jetbrains.kotlin.j2k.JavaToKotlinTranslator;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetPsiFactory;
import org.jetbrains.kotlin.ui.Activator;
import org.jetbrains.kotlin.ui.commands.CommandsPackage;
import org.jetbrains.kotlin.ui.commands.ConvertedKotlinData;
import org.jetbrains.kotlin.ui.formatter.AlignmentStrategy;
import org.jetbrains.kotlin.ui.launch.KotlinRuntimeConfigurationSuggestor;
import org.jetbrains.kotlin.wizards.FileCreationOp;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;

public class JavaToKotlinActionHandler extends AbstractHandler {
    private final static String DOC_START = "/**";
    private final static String DOC_ESCAPE_START = "/*%";
            
    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getActiveMenuSelection(event);
        if (selection instanceof IStructuredSelection) {
            Object[] elements = ((IStructuredSelection) selection).toArray();
            Set<CompilationUnit> elementsToKotlin = collectCompilationUnits(elements);
            Set<IProject> projects = CommandsPackage.getCorrespondingProjects(elementsToKotlin);
            
            Pair<IStatus, List<IFile>> result = convertToKotlin(elementsToKotlin, HandlerUtil.getActiveShell(event));
            if (result.getFirst().isOK()) {
                configureProjectsWithKotlin(projects);
                
                List<IFile> convertedFiles = result.getSecond();
                if (!convertedFiles.isEmpty()) {
                    CommandsPackage.openEditor(convertedFiles.get(0));
                }
            } else {
                MessageDialog.openError(HandlerUtil.getActiveShell(event), "Conversion error", result.getFirst().getMessage());
            }
        }
        
        return null;
    }
    
    @Override
    public void setEnabled(Object evaluationContext) {
        Object selection = HandlerUtil.getVariable(evaluationContext, ISources.ACTIVE_CURRENT_SELECTION_NAME);
        if (selection instanceof IStructuredSelection) {
            Object[] elements = ((IStructuredSelection) selection).toArray();
            Set<CompilationUnit> elementsToKotlin = collectCompilationUnits(elements);
            super.setBaseEnabled(!elementsToKotlin.isEmpty());
        } else {
            super.setBaseEnabled(false);
        }
    }
    
    private void closeEditors(@NotNull Set<CompilationUnit> units) {
        IEditorReference[] editorReferences = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getActivePage().getEditorReferences();
        for (IEditorReference editorReference : editorReferences) {
            IEditorPart editor = editorReference.getEditor(true);
            
            ITypeRoot root = EditorUtility.getEditorInputJavaElement(editor, true);
            if (root instanceof CompilationUnit) {
                if (units.contains(root)) {
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeEditor(editor, true);
                }
            }
        }
    }

    private void configureProjectsWithKotlin(@NotNull Set<IProject> projects) {
        for (IProject project : projects) {
            KotlinNature.addNature(project);
            KotlinRuntimeConfigurationSuggestor.suggestForProject(project);
        }
    }
    
    private Set<CompilationUnit> collectCompilationUnits(@NotNull Object[] selectedElements) {
        Set<CompilationUnit> elementsToKotlin = new LinkedHashSet<>();
        for (Object element : selectedElements) {
            if (element instanceof CompilationUnit) {
                CompilationUnit unit = (CompilationUnit) element;
                if (JavaCore.isJavaLikeFileName(unit.getElementName())) {
                    elementsToKotlin.add(unit);
                }
            } else if (element instanceof IPackageFragment) {
                elementsToKotlin.addAll(collectCompilationUnits((IPackageFragment) element));
            } else if (element instanceof IPackageFragmentRoot) {
                elementsToKotlin.addAll(collectCompilationUnits((IPackageFragmentRoot) element));
            }
        }
        
        return elementsToKotlin;
    }
    
    private List<CompilationUnit> collectCompilationUnits(@NotNull IPackageFragmentRoot packageFragmentRoot) {
        List<CompilationUnit> compilationUnits = new ArrayList<>();
        try {
            for (IJavaElement element : packageFragmentRoot.getChildren()) {
                if (element instanceof IPackageFragment) {
                    compilationUnits.addAll(collectCompilationUnits((IPackageFragment) element));
                }
            }
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return compilationUnits;
    }
    
    private List<CompilationUnit> collectCompilationUnits(@NotNull IPackageFragment packageFragment) {
        List<CompilationUnit> compilationUnits = new ArrayList<>();
        try {
            for (ICompilationUnit compilationUnit : packageFragment.getCompilationUnits()) {
                compilationUnits.add((CompilationUnit) compilationUnit);
            }
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return compilationUnits;
    }
    
    private Pair<IStatus, List<IFile>> convertToKotlin(@NotNull Set<CompilationUnit> compilationUnits, @NotNull Shell shell) {
        try {
            List<IFile> convertedFiles = new ArrayList<IFile>();
            CompositeUndoableOperation compositeOperation = new CompositeUndoableOperation("Convert Java to Kotlin");
            for (CompilationUnit compilationUnit : compilationUnits) {
                ConvertedKotlinData convertedFile = getConvertedFileData(compilationUnit, shell);
                compositeOperation.add(new CreateFileOperation(convertedFile.getFile(), null, 
                        new ByteArrayInputStream(convertedFile.getKotlinFileData().getBytes()), "Create Kotlin File"));
                
                convertedFiles.add(convertedFile.getFile());
            }
                
            DeleteResourcesOperation deleteCompilationUnits = CommandsPackage.getDeleteOperation(compilationUnits);
            compositeOperation.add(deleteCompilationUnits);
            
            closeEditors(compilationUnits);
            PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute(
                    compositeOperation, null, getUIInfoAdapter(shell));
            
            return new Pair<IStatus, List<IFile>>(Status.OK_STATUS, convertedFiles);
        } catch (ExecutionException e) {
            KotlinLogger.logError(e.getMessage(), null);
            return new Pair<IStatus, List<IFile>>(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()), Collections.<IFile>emptyList());
        }
    }
    
    private ConvertedKotlinData getConvertedFileData(@NotNull CompilationUnit compilationUnit, @NotNull Shell shell) throws ExecutionException {
        String contents = new String(compilationUnit.getContents()).replaceAll(Pattern.quote(DOC_START), DOC_ESCAPE_START);
        Project ideaProject = KotlinEnvironment.getEnvironment(compilationUnit.getJavaProject()).getProject();
        
        String translatedCode = JavaToKotlinTranslator.INSTANCE$.prettify(
                J2kPackage.translateToKotlin(contents, ideaProject));
        JetFile jetFile = getJetFile(translatedCode, compilationUnit.getJavaProject());
        String formattedCode = AlignmentStrategy.alignCode(jetFile.getNode(), getDefaultLineDelimiter(compilationUnit))
                .replaceAll(Pattern.quote(DOC_ESCAPE_START), DOC_START);
        
        String fileName = FileUtil.getNameWithoutExtension(compilationUnit.getElementName());
        IFile file = FileCreationOp.makeFile((IPackageFragment) compilationUnit.getParent(), compilationUnit.getPackageFragmentRoot(), fileName);
        if (file.exists()) {
            throw new ExecutionException("Could not convert file: " + compilationUnit.getElementName() + ". Because of existing file: " + file.getName());
        }
        
        return new ConvertedKotlinData(file, formattedCode);
    }
    
    private String getDefaultLineDelimiter(CompilationUnit unit) {
        try {
            IBuffer buffer = unit.getBuffer();
            IDocument document;
            if (buffer instanceof IDocument) {
                document = (IDocument) buffer;
            } else {
                document = new DocumentAdapter(buffer);
            }
            
            return TextUtilities.getDefaultLineDelimiter(document);
        } catch (JavaModelException e) {
            KotlinLogger.logAndThrow(e);
        }
        
        return null;
    }
    
    private JetFile getJetFile(@NotNull String sourceCode, @NotNull IJavaProject javaProject) {
        Project ideaProject = KotlinEnvironment.getEnvironment(javaProject).getProject();
        return new JetPsiFactory(ideaProject).createFile(sourceCode);
    }
}
