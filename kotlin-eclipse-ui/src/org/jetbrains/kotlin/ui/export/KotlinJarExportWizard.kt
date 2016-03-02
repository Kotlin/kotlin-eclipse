package org.jetbrains.kotlin.ui.export

import org.eclipse.jdt.internal.ui.jarpackagerfat.FatJarPackageWizard
import org.eclipse.jdt.ui.jarpackager.JarPackageData
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.jdt.internal.ui.jarpackagerfat.FatJarPackageWizardPage
import org.eclipse.core.runtime.MultiStatus
import java.util.ArrayList
import org.eclipse.swt.widgets.Combo
import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.jdt.core.JavaCore
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.jdt.core.IType

class KotlinJarExportWizard : FatJarPackageWizard() {
    override fun addPages() {
        val jarPackage = getPrivateField<JarPackageData>(this.javaClass.getSuperclass(), this, "fJarPackage")
        val selection = getPrivateField<IStructuredSelection>(this.javaClass.getSuperclass(), this, "fSelection")
        if (jarPackage == null || selection == null) return
        
        val page = KotlinJarExportWizardPage(jarPackage, selection)
        setPrivateField(this.javaClass.getSuperclass(), this, "fJarPackageWizardPage", page)
        
        addPage(page)
    }
}

class KotlinJarExportWizardPage(
        val jarPackage: JarPackageData, 
        selection: IStructuredSelection) : FatJarPackageWizardPage(jarPackage, selection) {
    override fun getSelectedElementsWithoutContainedChildren(status: MultiStatus): Array<Any> {
        val resultElements = super.getSelectedElementsWithoutContainedChildren(status)
        if (jarPackage.manifestMainClass == null) {
            jarPackage.manifestMainClass = findMainType(status)
        }
        
        return resultElements
    }
    
    private fun findMainType(status: MultiStatus): IType? {
        val mainTypeFqName = findMainTypeFqName(status)
        if (mainTypeFqName != null) {
            return findType(mainTypeFqName)
        }
        
        return null
    }
    
    private fun findType(typeFqName: String): IType? {
        return ResourcesPlugin.getWorkspace().getRoot().getProjects().asSequence()
                .map { JavaCore.create(it) }
                .mapNotNull { it.findType(typeFqName) }
                .firstOrNull()
    }
    
    private fun findMainTypeFqName(status: MultiStatus): String? {
        val launchConfiguration = getLaunchConfiguration()
        val mainClassMethod = this.javaClass.getDeclaredMethod("getMainClass", ILaunchConfiguration::class.java, MultiStatus::class.java)
        mainClassMethod.setAccessible(true)
        return mainClassMethod.invoke(this, launchConfiguration, status) as String?
    }
    
    private fun getLaunchConfiguration(): ILaunchConfiguration? {
        val configurationModel = getPrivateField<ArrayList<*>>(this.javaClass.getSuperclass(), this, "fLauchConfigurationModel")
        if (configurationModel == null) return null
        
        val combo = getPrivateField<Combo>(this.javaClass.getSuperclass(), this, "fLaunchConfigurationCombo")
        if (combo == null) return null
        
        val configurationElement = configurationModel.get(combo.selectionIndex)
        val launchConfigurationMethod = configurationElement.javaClass.getDeclaredMethod("getLaunchConfiguration")
        return launchConfigurationMethod.invoke(configurationElement) as ILaunchConfiguration
    }
}

private inline fun <reified RET> getPrivateField(obj: Class<*>, inst: Any, declaredFieldName: String): RET? {
    val field = obj.getDeclaredField(declaredFieldName)
    field.setAccessible(true)
    return field.get(inst) as RET?
}

private fun setPrivateField(obj: Class<*>, inst: Any, declaredFieldName: String, newValue: Any) {
    val field = obj.getDeclaredField(declaredFieldName)
    field.setAccessible(true)
    field.set(inst, newValue)
}