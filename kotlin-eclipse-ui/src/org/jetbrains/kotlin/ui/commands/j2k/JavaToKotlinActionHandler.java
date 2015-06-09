package org.jetbrains.kotlin.ui.commands.j2k;

import static org.eclipse.ui.ide.undo.WorkspaceUndoUtil.getUIInfoAdapter;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.undo.CreateFileOperation;
import org.eclipse.ui.ide.undo.DeleteResourcesOperation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.model.KotlinEnvironment;
import org.jetbrains.kotlin.j2k.J2kPackage;
import org.jetbrains.kotlin.j2k.JavaToKotlinTranslator;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetPsiFactory;
import org.jetbrains.kotlin.ui.Activator;
import org.jetbrains.kotlin.ui.formatter.AlignmentStrategy;
import org.jetbrains.kotlin.ui.launch.KotlinRuntimeConfigurationSuggestor;
import org.jetbrains.kotlin.wizards.FileCreationOp;
import org.jetbrains.kotlin.wizards.NewUnitWizard;

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
            List<CompilationUnit> elementsToKotlin = collectCompilationUnits(elements);
            Set<IProject> projects = getCorrespondingProjects(elementsToKotlin);
            
            IStatus status = convertToKotlin(elementsToKotlin, HandlerUtil.getActiveShell(event));
            if (status.isOK()) {
                configureProjectsWithKotlin(projects);
            } else {
                MessageDialog.openError(HandlerUtil.getActiveShell(event), "Conversion error", status.getMessage());
            }
        }
        
        return null;
    }
    
    @Override
    public void setEnabled(Object evaluationContext) {
        Object selection = HandlerUtil.getVariable(evaluationContext, ISources.ACTIVE_CURRENT_SELECTION_NAME);
        if (selection instanceof IStructuredSelection) {
            Object[] elements = ((IStructuredSelection) selection).toArray();
            List<CompilationUnit> elementsToKotlin = collectCompilationUnits(elements);
            super.setBaseEnabled(!elementsToKotlin.isEmpty());
        } else {
            super.setBaseEnabled(false);
        }
    }

    private void configureProjectsWithKotlin(@NotNull Set<IProject> projects) {
        for (IProject project : projects) {
            NewUnitWizard.addKotlinModelSpecificConfiguration(project);
            KotlinRuntimeConfigurationSuggestor.suggestForProject(project);
        }
    }
    
    private List<CompilationUnit> collectCompilationUnits(@NotNull Object[] selectedElements) {
        Set<CompilationUnit> elementsToKotlin = new HashSet<>();
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
        
        return new ArrayList<>(elementsToKotlin);
    }
    
    private Set<IProject> getCorrespondingProjects(@NotNull List<CompilationUnit> compilationUnits) {
        Set<IProject> projects = new HashSet<>();
        for (CompilationUnit compilationUnit : compilationUnits) {
            projects.add(compilationUnit.getJavaProject().getProject());
        }
        
        return projects;
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
    
    private IStatus convertToKotlin(@NotNull List<CompilationUnit> compilationUnits, @NotNull Shell shell) {
        try {
            CompositeUndoableOperation compositeOperation = new CompositeUndoableOperation("Convert Java to Kotlin");
            for (CompilationUnit compilationUnit : compilationUnits) {
                CreateFileOperation creationOperation = getConvertedFileCreationOperation(compilationUnit, shell);
                compositeOperation.add(creationOperation);
            }
                
            DeleteResourcesOperation deleteCompilationUnits = getDeleteOperation(compilationUnits, shell);
            compositeOperation.add(deleteCompilationUnits);
            
            PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute(
                    compositeOperation, null, getUIInfoAdapter(shell));
        } catch (ExecutionException e) {
            KotlinLogger.logError(e.getMessage(), null);
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage());
        }
        
        return Status.OK_STATUS;
    }
    
    private DeleteResourcesOperation getDeleteOperation(@NotNull List<CompilationUnit> compilationUnits, @NotNull Shell shell) throws ExecutionException {
        IResource[] resources = new IResource[compilationUnits.size()];
        for (int i = 0; i < resources.length; ++i) {
            resources[i] = compilationUnits.get(i).getResource();
        }
        return new DeleteResourcesOperation(resources, "Conversion files", true);
    }
    
    private CreateFileOperation getConvertedFileCreationOperation(@NotNull CompilationUnit compilationUnit, @NotNull Shell shell) throws ExecutionException {
        String contents = new String(compilationUnit.getContents()).replaceAll(Pattern.quote(DOC_START), DOC_ESCAPE_START);
        Project ideaProject = KotlinEnvironment.getEnvironment(compilationUnit.getJavaProject()).getProject();
        
        String translatedCode = JavaToKotlinTranslator.INSTANCE$.prettify(
                J2kPackage.translateToKotlin(contents, ideaProject));
        JetFile jetFile = getJetFile(translatedCode, compilationUnit.getJavaProject());
        String formattedCode = AlignmentStrategy.alignCode(jetFile.getNode()).replaceAll(Pattern.quote(DOC_ESCAPE_START), DOC_START);
        
        String fileName = FileUtil.getNameWithoutExtension(compilationUnit.getElementName());
        IFile file = FileCreationOp.makeFile((IPackageFragment) compilationUnit.getParent(), compilationUnit.getPackageFragmentRoot(), fileName);
        if (file.exists()) {
            throw new ExecutionException("Could not convert file: " + compilationUnit.getElementName() + ". Because of existing file: " + file.getName());
        }
        
        return new CreateFileOperation(file, null, new ByteArrayInputStream(formattedCode.getBytes()), "Create Kotlin File");
    }
    
    private JetFile getJetFile(@NotNull String sourceCode, @NotNull IJavaProject javaProject) {
        Project ideaProject = KotlinEnvironment.getEnvironment(javaProject).getProject();
        return new JetPsiFactory(ideaProject).createFile(sourceCode);
    }
}
