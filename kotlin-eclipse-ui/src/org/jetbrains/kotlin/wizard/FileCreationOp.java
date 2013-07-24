package org.jetbrains.kotlin.wizard;

import static org.eclipse.ui.ide.undo.WorkspaceUndoUtil.getUIInfoAdapter;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.undo.CreateFileOperation;
import org.jetbrains.kotlin.core.log.KotlinLogger;

class FileCreationOp implements IRunnableWithProgress {

    private final IPackageFragmentRoot sourceDir;
    private final IPackageFragment packageFragment;
    private final String unitName;
    private final String contents;
    private final Shell shell;

    private final static String ext = ".kt";

    private IFile result;

    IFile getResult() {
        return result;
    }

    FileCreationOp(IPackageFragmentRoot sourceDir, IPackageFragment packageFragment, String unitName,
            boolean includePreamble, String contents, Shell shell) {
        this.sourceDir = sourceDir;
        this.packageFragment = packageFragment;
        this.contents = contents;
        this.shell = shell;
        this.unitName = getCompilationUnitName(unitName);
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException {
        IPath path = packageFragment.getPath().append(unitName);
        IProject project = sourceDir.getJavaProject().getProject();
        result = project.getFile(path.makeRelativeTo(project.getFullPath()));
        try {
            if (!result.exists()) {
                CreateFileOperation op = new CreateFileOperation(result, null, null, "Create Kotlin Source File");
                PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute(op, monitor,
                        getUIInfoAdapter(shell));
                result.appendContents(new ByteArrayInputStream(contents.getBytes()), false, false, monitor);
            }
        } catch (ExecutionException e) {
            KotlinLogger.logAndThrow(e);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }

    static String getCompilationUnitName(String name) {
        if (name.endsWith(ext)) {
            return name;
        }
        return name + ext;
    }
}