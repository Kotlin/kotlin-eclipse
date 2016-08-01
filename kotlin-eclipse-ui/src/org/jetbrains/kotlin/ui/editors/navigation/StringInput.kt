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
package org.jetbrains.kotlin.ui.editors.navigation

import org.eclipse.core.resources.IStorage
import org.eclipse.jface.resource.ImageDescriptor
import org.eclipse.ui.IPersistableElement
import org.eclipse.ui.IStorageEditorInput
import org.jetbrains.kotlin.psi.KtFile

open class StringInput internal constructor(private val storage: StringStorage) : IStorageEditorInput {
    override fun exists(): Boolean {
        return true
    }

    override fun getImageDescriptor(): ImageDescriptor? = null

    override fun getName(): String? = storage.name

    override fun getPersistable(): IPersistableElement? = null

    override fun getStorage(): IStorage? = storage

    override fun getToolTipText(): String? = storage.fqName

    override fun <T> getAdapter(required: Class<T>?): T? = storage.getAdapter(required)

    override fun equals(other: Any?): Boolean {
        if (other is StringInput) {
            return storage.equals(other.storage)
        }
        return false
    }

    override fun hashCode(): Int = storage.hashCode()
}

class KotlinExternalEditorInput(val ktFile: KtFile, storage: StringStorage) : StringInput(storage)