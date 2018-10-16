package org.jetbrains.kotlin.maven.configuration

import org.apache.maven.model.Plugin
import org.apache.maven.project.MavenProject
import org.codehaus.plexus.util.xml.Xpp3Dom
import java.util.*

class MavenAttributeAccessor(private val attributeName: String? = null, private val propertyPath: String? = null) {
    fun getFrom(project: MavenProject, plugin: Plugin): String? =
        getFromConfiguration(plugin.configuration) ?: getFromProperties(project.properties)

    private fun getFromConfiguration(configuration: Any?): String? =
        (configuration as? Xpp3Dom)
            ?.getChild(attributeName)
            ?.value

    private fun getFromProperties(properties: Properties): String? =
            propertyPath?.let { properties[it] as? String }
}