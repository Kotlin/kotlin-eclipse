package org.jetbrains.kotlin.wizard;

import static org.eclipse.ui.ide.undo.WorkspaceUndoUtil.getUIInfoAdapter;

import java.io.ByteArrayInputStream;
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

class FileCreationOp implements IRunnableWithProgress {

    private final IPackageFragmentRoot sourceDir;
    private final IPackageFragment packageFragment;
    private final String unitName;
    private final String contents;
    private final Shell shell;
    
    private IFile result;
    
    IFile getResult() {
        return result;
    }
    
    FileCreationOp(IPackageFragmentRoot sourceDir,
            IPackageFragment packageFragment, String unitName,
            boolean includePreamble, String contents, Shell shell) {
        this.sourceDir = sourceDir;
        this.packageFragment = packageFragment;
        this.unitName = unitName;
        this.contents = contents;
        this.shell = shell;
    }
    
    public void run(IProgressMonitor monitor) {
        IPath path = packageFragment.getPath().append(unitName + ".kt");
        IProject project = sourceDir.getJavaProject().getProject();
        result = project.getFile(path.makeRelativeTo(project.getFullPath()));
        try {
            if (!result.exists()) {
                CreateFileOperation op = new CreateFileOperation(result, null, 
                        null, "Create Kotlin Source File");
                try {
                    PlatformUI.getWorkbench().getOperationSupport().getOperationHistory()
                    .execute(op, monitor, getUIInfoAdapter(shell));
                } 
                catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }        
            result.appendContents(new ByteArrayInputStream(contents.getBytes()), 
                    false, false, monitor);
        }
        catch (CoreException e) {
            e.printStackTrace();
        }
    }
}