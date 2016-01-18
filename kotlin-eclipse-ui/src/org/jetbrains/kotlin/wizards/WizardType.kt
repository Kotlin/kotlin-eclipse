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
package org.jetbrains.kotlin.wizards;

public enum WizardType {
    NONE {
        @Override
        String getWizardTypeName() {
            return "Source";
        }

        @Override
        String getFileBodyFormat() {
            return "";
        }
    },
    CLASS {
        @Override
        String getWizardTypeName() {
            return "Class";
        }

        @Override
        String getFileBodyFormat() {
            return PUBLIC_MODIFIER + "class" + NOT_EMPTY_BODY_FORMAT;
        }
    },
    TRAIT {
        @Override
        String getWizardTypeName() {
            return "Trait";
        }

        @Override
        String getFileBodyFormat() {
            return PUBLIC_MODIFIER + "trait" + NOT_EMPTY_BODY_FORMAT;
        }
    },
    OBJECT {
        @Override
        String getWizardTypeName() {
            return "Object";
        }

        @Override
        String getFileBodyFormat() {
            return PUBLIC_MODIFIER + "object" + NOT_EMPTY_BODY_FORMAT;
        }
    },
    ENUM {
        @Override
        String getWizardTypeName() {
            return "Enum";
        }

        @Override
        String getFileBodyFormat() {
            return PUBLIC_MODIFIER + "enum class" + NOT_EMPTY_BODY_FORMAT;
        }
    };
    
    private static final String PUBLIC_MODIFIER = "public ";
    private static final String NOT_EMPTY_BODY_FORMAT = " %s {\n}";
    
    abstract String getWizardTypeName();
    
    abstract String getFileBodyFormat();
}
