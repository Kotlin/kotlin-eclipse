package org.jetbrains.kotlin.maven.configuration;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.AbstractSourcesGenerationProjectConfigurator;
import org.eclipse.m2e.jdt.IClasspathDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.core.KotlinClasspathContainer;
import org.jetbrains.kotlin.core.model.KotlinNature;

public class KotlinMavenProjectConfigurator extends AbstractSourcesGenerationProjectConfigurator {
	private static final String GROUP_ID = "org.jetbrains.kotlin";
    private static final String MAVEN_PLUGIN_ID = "kotlin-maven-plugin";
    
	@Override
	public void configureRawClasspath(ProjectConfigurationRequest request, IClasspathDescriptor classpath,
		      IProgressMonitor monitor)
			throws CoreException {
		if (hasKotlinMavenPlugin(request.getMavenProject())) {
			classpath.addEntry(KotlinClasspathContainer.CONTAINER_ENTRY);
			addNature(request.getProject(), KotlinNature.KOTLIN_NATURE, monitor);
		}
	}

	private boolean hasKotlinMavenPlugin(@NotNull MavenProject mavenProject) {
		for (Plugin buildPlugin : mavenProject.getBuildPlugins()) {
			if (checkCoordinates(buildPlugin, GROUP_ID, MAVEN_PLUGIN_ID)) {
				return true;
			}
		}
		
		return false;
	}
	
	private boolean checkCoordinates(
            @NotNull Plugin buildPlugin,
            @NotNull String groupId,
            @NotNull String artifactId
    ) {
        return groupId.equals(buildPlugin.getGroupId()) && artifactId.equals(buildPlugin.getArtifactId());
    }
}