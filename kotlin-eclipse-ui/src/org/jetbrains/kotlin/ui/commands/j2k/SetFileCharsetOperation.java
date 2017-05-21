package org.jetbrains.kotlin.ui.commands.j2k;

import static org.jetbrains.kotlin.ui.Activator.PLUGIN_ID;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class SetFileCharsetOperation extends AbstractOperation {

    private static final String SET_FILE_CHARSET_OPERATION_TITLE = "Set file charset";

    private final IFile file;
    private String charset; 
    private boolean applied;

    public SetFileCharsetOperation(IFile file, String charset) {
        super(SET_FILE_CHARSET_OPERATION_TITLE);
        this.file = file;
        this.charset = charset;
        this.applied = false;
    }

    @Override
    public boolean canExecute() {
        return !applied;
    }

    @Override
    public boolean canRedo() {
        return canExecute();
    }

    @Override
    public boolean canUndo() {
        return applied;
    }

    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        return toggleEncoding(monitor, info);
    }

    @Override
    public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        return execute(monitor, info);
    }

    @Override
    public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        return toggleEncoding(monitor, info);
    }

    private IStatus toggleEncoding(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        try {
            String previousCharset = file.getCharset();
            file.setCharset(charset, monitor);
            charset = previousCharset;
            applied = !applied;
            return Status.OK_STATUS;
        } catch (CoreException e) {
            return new Status(IStatus.ERROR, PLUGIN_ID, "Unable to change file charset.", e);
        }
    }

}
