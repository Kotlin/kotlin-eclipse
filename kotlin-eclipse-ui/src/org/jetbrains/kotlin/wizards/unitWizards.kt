/*******************************************************************************
 * Copyright 2000-2016 JetBrains s.r.o.
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
@file:Suppress("unused")

package org.jetbrains.kotlin.wizards

import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.ui.IWorkbench
import org.eclipse.ui.dialogs.WizardNewFileCreationPage
import org.eclipse.ui.ide.IDE
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard
import org.jetbrains.kotlin.parsing.KotlinParserDefinition

class NewClassWizard : NewUnitWizard(WizardType.CLASS)
class NewSealedClassWizard : NewUnitWizard(WizardType.SEALED_CLASS)
class NewEnumWizard : NewUnitWizard(WizardType.ENUM)
class NewDataClassWizard : NewUnitWizard(WizardType.DATA)
class NewAnnotationWizard : NewUnitWizard(WizardType.ANNOTATION)
class NewObjectWizard : NewUnitWizard(WizardType.OBJECT)
class NewInterfaceWizard : NewUnitWizard(WizardType.INTERFACE)
class NewSealedInterfaceWizard : NewUnitWizard(WizardType.SEALED_INTERFACE)

class NewScriptWizard : BasicNewResourceWizard() {
    companion object {
        private const val pageName = "New Kotlin Script"
    }

    private lateinit var mainPage: WizardNewFileCreationPage

    override fun addPages() {
        super.addPages()

        mainPage = WizardNewFileCreationPage(pageName, getSelection()).apply {
            fileExtension = KotlinParserDefinition.STD_SCRIPT_SUFFIX
            title = "Kotlin Script"
            description = "Create a new Kotlin script"
        }

        addPage(mainPage)
    }

    override fun init(workbench: IWorkbench, currentSelection: IStructuredSelection) {
        super.init(workbench, currentSelection)
        windowTitle = pageName
    }

    override fun performFinish(): Boolean {
        val file = mainPage.createNewFile() ?: return false

        selectAndReveal(file)
        workbench.activeWorkbenchWindow?.let {
            val page = it.activePage
            if (page != null) {
                IDE.openEditor(page, file, true)
            }
        }

        return true
    }
}
