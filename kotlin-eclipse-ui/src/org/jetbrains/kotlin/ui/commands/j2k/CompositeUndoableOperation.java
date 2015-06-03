package org.jetbrains.kotlin.ui.commands.j2k;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.commands.operations.ICompositeOperation;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.ide.undo.WorkspaceUndoUtil;

public class CompositeUndoableOperation extends AbstractOperation implements ICompositeOperation {

    private final List<IUndoableOperation> operations = new ArrayList<>();
    
    public CompositeUndoableOperation(String name) {
        super(name);
        this.addContext(WorkspaceUndoUtil.getWorkspaceUndoContext());
    }

    
    @Override
    public void add(IUndoableOperation operation) {
        operations.add(operation);
    }

    @Override
    public void remove(IUndoableOperation operation) {
        operations.remove(operation);
    }

    @Override
    public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        IStatus status = Status.OK_STATUS;
        for (IUndoableOperation operation : operations) {
            IStatus executionStatus = operation.execute(monitor, info);
            if (!executionStatus.isOK()) {
                status = executionStatus;
            }
        }
        
        return status;
    }
    
    protected IWorkspace getWorkspace() {
        return ResourcesPlugin.getWorkspace();
    }

    @Override
    public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        for (IUndoableOperation operation : operations) {
            if (operation.canRedo()) {
                IStatus redoStatus = operation.redo(monitor, info);
                if (!redoStatus.isOK()) {
                    return redoStatus;
                }
            }
        }
        
        return Status.OK_STATUS;
    }

    @Override
    public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        for (IUndoableOperation operation : operations) {
            if (operation.canUndo()) {
                IStatus undoStatus = operation.undo(monitor, info);
                if (!undoStatus.isOK()) {
                    return undoStatus;
                }
            }
        }
        
        return Status.OK_STATUS;
    }
}
