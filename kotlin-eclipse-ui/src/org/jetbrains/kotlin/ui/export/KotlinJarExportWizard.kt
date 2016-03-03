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
import org.eclipse.jdt.internal.ui.JavaPlugin
import org.eclipse.core.runtime.IStatus
import org.eclipse.jdt.internal.ui.jarpackagerfat.FatJarPackagerMessages

class KotlinJarExportWizard : FatJarPackageWizard() {
    override fun addPages() {
        val jarPackage = getPrivateField(this.javaClass.getSuperclass(), this, "fJarPackage") as? JarPackageData
        val selection = getPrivateField(this.javaClass.getSuperclass(), this, "fSelection") as? IStructuredSelection
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
        val javaStatus = MultiStatus(JavaPlugin.getPluginId(), IStatus.OK, FatJarPackagerMessages.FatJarPackageWizard_JarExportProblems_message, null);
        val resultElements = super.getSelectedElementsWithoutContainedChildren(javaStatus)
        if (jarPackage.manifestMainClass == null) {
            jarPackage.manifestMainClass = findMainType(status)
        }
        
        if (jarPackage.manifestMainClass == null) {
            status.addAll(javaStatus)
        } else {
            val correctedStatus = javaStatus.children.filter { it.message != FatJarPackagerMessages.FatJarPackageWizardPage_error_noMainMethod }
            correctedStatus.forEach { status.add(it) }
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
        val mainClassMethod = this.javaClass.getSuperclass().getDeclaredMethod("getMainClass", ILaunchConfiguration::class.java, MultiStatus::class.java)
        mainClassMethod.setAccessible(true)
        return mainClassMethod.invoke(this, launchConfiguration, status) as String?
    }
    
    private fun getLaunchConfiguration(): ILaunchConfiguration? {
        val configurationModel = getPrivateField(this.javaClass.getSuperclass(), this, "fLauchConfigurationModel") as? ArrayList<*>
        if (configurationModel == null) return null
        
        val combo = getPrivateField(this.javaClass.getSuperclass(), this, "fLaunchConfigurationCombo") as? Combo
        if (combo == null) return null
        
        val configurationElement = configurationModel.get(combo.selectionIndex)
        val launchConfigurationMethod = configurationElement.javaClass.getDeclaredMethod("getLaunchConfiguration")
        launchConfigurationMethod.setAccessible(true)
        return launchConfigurationMethod.invoke(configurationElement) as ILaunchConfiguration
    }
}

private fun getPrivateField(obj: Class<*>, inst: Any, declaredFieldName: String): Any? {
    val field = obj.getDeclaredField(declaredFieldName)
    field.setAccessible(true)
    return field.get(inst)
}

private fun setPrivateField(obj: Class<*>, inst: Any, declaredFieldName: String, newValue: Any) {
    val field = obj.getDeclaredField(declaredFieldName)
    field.setAccessible(true)
    field.set(inst, newValue)
}