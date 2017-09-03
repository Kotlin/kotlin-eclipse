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
package org.jetbrains.kotlin.ui.launch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.core.launch.CompilerOutputData;
import org.jetbrains.kotlin.core.launch.CompilerOutputElement;

public class CompilerStatusHandler implements IStatusHandler {
    
    public static final String CONSOLE_NAME = "org.jetbrains.kotlin.ui.console";
    
    private static final RGB CONSOLE_RED = new RGB(229, 43, 80);
    private static final RGB CONSOLE_YELLOW = new RGB(218, 165, 32);
    private static final RGB CONSOLE_BLACK = new RGB(0, 0, 0);

    @Override
    public Object handleStatus(IStatus status, Object source) throws CoreException {
        if (!(source instanceof CompilerOutputData)) {
            return null;
        }
        
        List<CompilerOutputElement> outputDataList = ((CompilerOutputData) source).getList(); 
        
        Map<String, List<CompilerOutputElement>> sortedOutput = groupOutputByPath(outputDataList);
        
        MessageConsole msgConsole = KotlinConsoleKt.createCleanKotlinConsole();
        for (List<CompilerOutputElement> outputList : sortedOutput.values()) {
            printCompilerOutputList(outputList, msgConsole);
        }
        
        if (status.getSeverity() == IStatus.ERROR) {
            ConsolePlugin.getDefault().getConsoleManager().showConsoleView(msgConsole);
        }
        
        return null;
    }
    
    private void printCompilerOutputList(List<CompilerOutputElement> outputList, MessageConsole msgConsole) {
        printlnToConsole(outputList.get(0).getMessageLocation().getPath(), CONSOLE_BLACK, msgConsole);
        
        for (CompilerOutputElement dataElement : outputList) {
            RGB color = getColorByMessageSeverity(dataElement.getMessageSeverity());
            StringBuilder message = new StringBuilder();
            
            message.append("\t");
            message.append(dataElement.getMessageSeverity().toString() + ": " + dataElement.getMessage());
            if (dataElement.getMessageLocation() != null) {
                message.append(" (" + dataElement.getMessageLocation().getLine() + ", " + dataElement.getMessageLocation().getColumn() + ")");
            }
            
            printlnToConsole(message.toString(), color, msgConsole);
        }
    }
    
    private RGB getColorByMessageSeverity(CompilerMessageSeverity messageSeverity) {
        RGB color = null;
        switch (messageSeverity) {
        case ERROR:
            color = CONSOLE_RED;
            break;
        case WARNING:
            color = CONSOLE_YELLOW;
            break;
        default:
            color = CONSOLE_BLACK;
            break;
        }
        
        return color;
    }
    
    private void printlnToConsole(String message, RGB color, MessageConsole msgConsole) {
        MessageConsoleStream msgStream = msgConsole.newMessageStream();
        msgStream.setColor(new Color(null, color));
        
        msgStream.println(message);
    }
    
    private Map<String, List<CompilerOutputElement>> groupOutputByPath(List<CompilerOutputElement> outputData) {
        Map<String, List<CompilerOutputElement>> res = new HashMap<String, List<CompilerOutputElement>>();
        for (CompilerOutputElement dataElement : outputData) {
            String path = dataElement.getMessageLocation().getPath();
            if (!res.containsKey(path)) {
                res.put(path, new ArrayList<CompilerOutputElement>());
            }
            
            res.get(path).add(dataElement);
        }
        
        return res;
    }
}