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
import org.eclipse.core.runtime.IPath
import java.io.InputStream

data class StringStorage(
        private val content: String,
        private val name: String,
        private val packageFqName: String) : IStorage {
    
    override fun getName(): String = name

    override fun getContents(): InputStream? = content.byteInputStream()

    override fun getFullPath(): IPath? = null

    override fun <T> getAdapter(adapter: Class<T>?): T? = null

    override fun isReadOnly(): Boolean = true

    val fqName: String
        get() = "$packageFqName/$name"
}