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
package org.jetbrains.kotlin.ui;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jetbrains.kotlin.core.utils.KotlinFilesCollectorUtilsKt;
import org.jetbrains.kotlin.ui.builder.KotlinClassPathListener;
import org.jetbrains.kotlin.ui.builder.KotlinJavaDeclarationsListener;
import org.jetbrains.kotlin.ui.builder.ResourceChangeListener;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
    // The plug-in ID
    public static final String PLUGIN_ID = "org.jetbrains.kotlin.ui"; //$NON-NLS-1$
    
    // The shared instance
    private static Activator plugin;
    
    private final IResourceChangeListener resourceChangeListener = new ResourceChangeListener();
    private final IElementChangedListener kotlinClassPathChangedListener = new KotlinClassPathListener();
    private final IElementChangedListener kotlinJavaDeclarationsListener = new KotlinJavaDeclarationsListener();
    
    public Activator() {
    }
    
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        
        KotlinFilesCollectorUtilsKt.addFilesToParseFromKotlinProjectsInWorkspace();
        
        ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.POST_CHANGE);
        JavaCore.addElementChangedListener(kotlinClassPathChangedListener);
        JavaCore.addElementChangedListener(kotlinJavaDeclarationsListener);
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
        JavaCore.removeElementChangedListener(kotlinClassPathChangedListener);
        JavaCore.removeElementChangedListener(kotlinJavaDeclarationsListener);
        
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
