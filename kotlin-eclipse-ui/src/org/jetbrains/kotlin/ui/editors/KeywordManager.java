package org.jetbrains.kotlin.ui.editors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public final class KeywordManager {
    private enum KeywordGroup {
        Declare,
        TypeConstants,
        Other
    }
 
    private final static HashMap<KeywordGroup, List<String>> keywordCollection = new HashMap<KeywordGroup, List<String>>(); 
    
    static {
        final String[] declareKeywords = {
                "class", "fun", "package", "trait", "type", "val", "var"};
        keywordCollection.put(KeywordGroup.Declare, Arrays.asList(declareKeywords));
        
        final String[] otherKeywords = {
                "as", "this", "super", "for", "is", 
                "in", "throw", "return", "break", "continue", "object", 
                "if", "try", "else", "while", "do", "when", "This"};
        keywordCollection.put(KeywordGroup.Other, Arrays.asList(otherKeywords));
        
        final String[] typeConstants = {"null", "true", "false"};
        keywordCollection.put(KeywordGroup.TypeConstants, Arrays.asList(typeConstants));
    }
    
    /**
     * Get the declare keywords.
     * @return list of declare keywords.
     */
    public static Iterable<String> getDeclareKeywords() {
        return keywordCollection.get(KeywordGroup.Declare);
    }
    
    /**
     * Get the non-declare keywords.
     * @return list of non-declare keywords.
     */
    public static Iterable<String> getNonDeclareKeywords() {
        return keywordCollection.get(KeywordGroup.Other);
    }
    
    /**
     * Get all declare keywords.
     * @return list of all keywords.
     */
    public static Iterable<String> getAllKeywords() {
        List<String> all = new ArrayList<String>();
        all.addAll(keywordCollection.get(KeywordGroup.Declare));
        all.addAll(keywordCollection.get(KeywordGroup.Other));
        all.addAll(keywordCollection.get(KeywordGroup.TypeConstants));
        return all;
    }
    
    /**
     * Get the constant keywords.
     * @return list of constant keywords.
     */
    public static Iterable<String> getConstantKeywords() {
        return keywordCollection.get(KeywordGroup.TypeConstants);
    }
}
