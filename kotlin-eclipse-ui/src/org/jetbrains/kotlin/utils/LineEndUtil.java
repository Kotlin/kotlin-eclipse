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
package org.jetbrains.kotlin.utils;

public class LineEndUtil {
    
    public static final char CARRIAGE_RETURN_CHAR = '\r';
    public static final String CARRIAGE_RETURN_STRING = Character.toString(CARRIAGE_RETURN_CHAR);
    public static final char NEW_LINE_CHAR = '\n';
    public static final String NEW_LINE_STRING = Character.toString(NEW_LINE_CHAR);
    
    public static int convertLfToOsOffset(String lfText, int lfOffset) {
        String osLineSeparator = System.lineSeparator();
        if (osLineSeparator.length() == 1) {
            return lfOffset;
        }
        
        assertLineSeparator(osLineSeparator);
        
        // In CrLf move to new line takes 2 char instead of 1 in Lf
        return lfOffset + offsetToLineNumber(lfText, lfOffset);
    }

    private static int offsetToLineNumber(String lfText, int offset) {
        int line = 0;
        int curOffset = 0;
        
        while (curOffset < offset) {
            if (curOffset == lfText.length()) {
                break;
            }
            
            char c = lfText.charAt(curOffset);
            if (c == NEW_LINE_CHAR) {
                line++;
            } else if (c == CARRIAGE_RETURN_CHAR) {
                throw new IllegalArgumentException("Given text shouldn't contain \\r char");
            }
            
            curOffset++;
        }
        
        return line;
    }
    
    public static int convertCrToOsOffset(String crText, int crOffset) {
        String osLineSeparator = System.lineSeparator();
        if (osLineSeparator.length() == 1) {
            return crOffset;
        }
        
        assertLineSeparator(osLineSeparator);
        
        return crOffset - countCrToLineNumber(crText, crOffset);
    }
    
    private static int countCrToLineNumber(String lfText, int offset) {
        int countCR = 0;
        int curOffset = 0;
        
        while (curOffset < offset) {
            if (curOffset == lfText.length()) {
                break;
            }
            
            char c = lfText.charAt(curOffset);
            if (c == CARRIAGE_RETURN_CHAR) {
                countCR++;
            } 
            
            curOffset++;
        }
        
        return countCR;  
    }
    
    private static void assertLineSeparator(String osLineSeparator) {
        assert osLineSeparator.equals(CARRIAGE_RETURN_STRING + NEW_LINE_STRING) : "Only \r\n is expected as multi char line separator";
    }
}
