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
package org.jetbrains.kotlin.core.model

import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IConfigurationElement
import org.eclipse.core.runtime.Platform
import org.jetbrains.kotlin.core.log.KotlinLogger
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

fun <T> loadExecutableEP(extensionPointId: String, isCaching: Boolean = false): List<ExecutableExtensionPointDescriptor<T>> {
    return Platform
            .getExtensionRegistry()
            .getConfigurationElementsFor(extensionPointId)
            .map { ExecutableExtensionPointDescriptor<T>(it, isCaching) }
}

class ExecutableExtensionPointDescriptor<T>(
    private val configurationElement: IConfigurationElement,
    private val isCaching: Boolean = false
) {
    companion object {
        private const val CLASS = "class"
    }

    private var cachedProvider: T? = null

    val attributes: Map<String, String> = configurationElement.run {
        attributeNames
            .map { it to getAttribute(it) }
            .toMap()
    }

    @Suppress("UNCHECKED_CAST")
    fun createProvider(): T? {
        return cachedProvider ?: try {
            val provider = (configurationElement.createExecutableExtension(CLASS) as T?)
            if (isCaching) {
                cachedProvider = provider
            }
            provider
        } catch(e: CoreException) {
            KotlinLogger.logError(e)
            null
        }
    }

    fun providedClass(): KClass<out Any>? = configurationElement.getAttribute(CLASS)?.let { Class.forName(it).kotlin }
}

object EPAttribute {
    operator fun getValue(thisRef: ExecutableExtensionPointDescriptor<*>, property: KProperty<*>): String? =
            thisRef.attributes[property.name]
}