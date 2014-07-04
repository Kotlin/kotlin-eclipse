/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
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
 *
 *******************************************************************************/
package org.jetbrains.kotlin.wizards;

import static org.eclipse.ui.ide.undo.WorkspaceUndoUtil.getUIInfoAdapter;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.jetbrains.kotlin.core.log.KotlinLogger;

public class ProjectCreationOp implements IRunnableWithProgress {
    
    private final IProjectDescription projectDescription;
    private final String projectName;
    private final Shell shell;
    
    private IProject result;
    
    public ProjectCreationOp(IProjectDescription projectDescription, String projectName, Shell shell) {
        this.projectDescription = projectDescription;
        this.projectName = projectName;
        this.shell = shell;
    }
    
    public IProject getResult() {
        return result;
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        CreateProjectOperation operation = new CreateProjectOperation(projectDescription, projectName);
        try {
            PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute(operation, monitor,
                    getUIInfoAdapter(shell));
        } catch (ExecutionException e) {
            KotlinLogger.logAndThrow(e);
        }
    }

}
