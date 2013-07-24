package org.jetbrains.kotlin.ui;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jetbrains.kotlin.core.utils.KotlinFilesCollector;
import org.jetbrains.kotlin.ui.editors.AnalyzerScheduler;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
    // The plug-in ID
    public static final String PLUGIN_ID = "org.jetbrains.kotlin.ui"; //$NON-NLS-1$

    // The shared instance
    private static Activator plugin;

    public Activator() {
        KotlinFilesCollector.collectForParsing();
        analyzeAllProjects();
    }
    
    private void analyzeAllProjects() {
        try {
            JavaModelManager modelManager = JavaModelManager.getJavaModelManager();
            IJavaProject[] javaProjects = modelManager.getJavaModel().getJavaProjects();
            
            for (IJavaProject javaProject : javaProjects) {
                AnalyzerScheduler.analyzeProjectInBackground(javaProject);
            }
        } catch (JavaModelException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }
}
