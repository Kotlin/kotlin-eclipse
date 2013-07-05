package org.jetbrains.kotlin.core.launch;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity;

public class CompilerOutputData {
    
    private final List<CompilerOutputElement> data = new ArrayList<CompilerOutputElement>();
    
    public void add(CompilerMessageSeverity messageSeverity, String message, CompilerMessageLocation messageLocation) {
        data.add(new CompilerOutputElement(messageSeverity, message, messageLocation));
    }
    
    public void clear() {
        data.clear();
    }
    
    public List<CompilerOutputElement> getList() {
        return data;
    }
}