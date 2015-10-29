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
package org.jetbrains.kotlin.ui.editors.templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.jetbrains.kotlin.core.model.KotlinEnvironment;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtPsiFactory;
import org.jetbrains.kotlin.ui.formatter.AlignmentStrategy;

import com.intellij.openapi.project.Project;

public class KotlinTemplateFormatter {
    
    // Source code has been taken from EcliHX plugin
    // eclihx.ui.internal.ui.editors.templates.TemplateFormatter
    private static class VariableOffsetsTracker {

        private static final int MARKER_SIZE = 6;
        private static final String MARKER_PREFIX = "##__";
        private static final String MARKER_FORMAT_STRING = "##__%02d";

        static {
            assert MARKER_FORMAT_STRING.startsWith(MARKER_PREFIX);
            assert String.format(MARKER_FORMAT_STRING, 0).length() == MARKER_SIZE;
        }

        private final String markedString;

        // Marker id to variable
        private final Map<Integer, TemplateVariable> variablesIds = new HashMap<Integer, TemplateVariable>();

        private String unmarkedString;
        private TemplateVariable[] updatedVariables;

        public VariableOffsetsTracker(String text, TemplateVariable[] variables) {

            // Collect variables offsets
            SortedMap<Integer, TemplateVariable> initialOffsets = new TreeMap<Integer, TemplateVariable>();
            for (TemplateVariable variable : variables) {
                for (int offset : variable.getOffsets()) {
                    initialOffsets.put(offset, variable);
                }
            }

            int id = 0;
            StringBuffer buffer = new StringBuffer(text);

            for (Entry<Integer, TemplateVariable> pair : initialOffsets.entrySet()) {
                int originalOffset = pair.getKey();
                TemplateVariable templateVariable = pair.getValue();

                int newOffset = originalOffset + id * MARKER_SIZE;

                buffer.insert(newOffset, String.format(MARKER_FORMAT_STRING, id));
                variablesIds.put(id, templateVariable);

                id++;
            }

            markedString = buffer.toString();
        }

        public void unmark(String formattedString) {
            StringBuffer unmarkedBuffer = new StringBuffer(formattedString);

            int variableOffset = unmarkedBuffer.indexOf(MARKER_PREFIX);

            // Collection on new offsets for each variable
            final Map<TemplateVariable, ArrayList<Integer>> variablesOffsets = new HashMap<TemplateVariable, ArrayList<Integer>>();

            // Remove markers and collect information about offsets.
            while (variableOffset != -1) {
                String idString = unmarkedBuffer.substring(variableOffset + MARKER_PREFIX.length(), variableOffset
                        + MARKER_SIZE);
                int id = Integer.parseInt(idString);

                TemplateVariable variable = variablesIds.get(id);

                if (!variablesOffsets.containsKey(variable)) {
                    variablesOffsets.put(variable, new ArrayList<Integer>());
                }

                variablesOffsets.get(variable).add(variableOffset);

                // Remove marker
                unmarkedBuffer.replace(variableOffset, variableOffset + MARKER_SIZE, "");

                // Update loop condition variable - move to next variable
                variableOffset = unmarkedBuffer.indexOf(MARKER_PREFIX, variableOffset);
            }

            // Update variables
            for (Entry<TemplateVariable, ArrayList<Integer>> variableToOffsets : variablesOffsets.entrySet()) {
                TemplateVariable variable = variableToOffsets.getKey();
                ArrayList<Integer> offsets = variableToOffsets.getValue();

                variable.setOffsets(toIntArray(offsets));
            }

            // Store results
            unmarkedString = unmarkedBuffer.toString();
            updatedVariables = variablesOffsets.keySet().toArray(new TemplateVariable[variablesOffsets.keySet().size()]);
        }

        public String getMarkedString() {
            return markedString;
        }

        public String getUnmarkedString() {
            if (unmarkedString == null) {
                throw new IllegalStateException("should be unmarked before");
            }

            return unmarkedString;
        }

        public TemplateVariable[] getCorrectedVariables() {
            if (updatedVariables == null) {
                throw new IllegalStateException("should be unmarked before");
            }

            return updatedVariables;
        }

        private static int[] toIntArray(List<Integer> list) {
            int[] ret = new int[list.size()];
            for (int i = 0; i < ret.length; i++)
                ret[i] = list.get(i);
            return ret;
        }
    }
    
    public void format(TemplateBuffer buffer, int lineIndentation, String lineDelimiter, IJavaProject javaProject) { 
        VariableOffsetsTracker offsetsTracker = new VariableOffsetsTracker(buffer.getString(), buffer.getVariables());
        Project ideaProject = KotlinEnvironment.getEnvironment(javaProject).getProject();
        KtFile parsedFile = new KtPsiFactory(ideaProject).createFile(offsetsTracker.getMarkedString());
        
        assert parsedFile != null;

        String formatted = AlignmentStrategy.alignCode(parsedFile.getNode(), lineIndentation, lineDelimiter);
        
        offsetsTracker.unmark(formatted);
        
        buffer.setContent(offsetsTracker.getUnmarkedString(), offsetsTracker.getCorrectedVariables());
    } 
}
