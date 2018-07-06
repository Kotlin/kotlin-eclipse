/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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
package org.jetbrains.kotlin.core.resolve.lang.java.structure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.load.java.components.JavaPropertyInitializerEvaluator;
import org.jetbrains.kotlin.load.java.structure.JavaField;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.constants.ConstantValueFactory;

public class EclipseJavaPropertyInitializerEvaluator implements JavaPropertyInitializerEvaluator {
    @Override
    @Nullable
    public ConstantValue<?> getInitializerConstant(@NotNull JavaField field, @NotNull PropertyDescriptor descriptor) {
        Object evaluated = field.getInitializerValue();
        if (evaluated == null)
            return null;
        // Note: evaluated expression may be of class that does not match field type in
        // some cases
        // tested for Int, left other checks just in case
        if (evaluated instanceof Byte || evaluated instanceof Short || evaluated instanceof Integer
                || evaluated instanceof Long)
            return ConstantValueFactory.INSTANCE.createIntegerConstantValue(((Number) evaluated).longValue(),
                    descriptor.getType(),
                    false);
        else
            return ConstantValueFactory.INSTANCE.createConstantValue(evaluated);
    }
}
