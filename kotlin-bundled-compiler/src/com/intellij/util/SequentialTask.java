package com.intellij.util;

public interface SequentialTask {
    
    /**
     * Callback method that is assumed to be called before the processing.
     */
    void prepare();
    
    /**
     * @return <code>true</code> if the processing is complete;
     *         <code>false</code> otherwise
     */
    boolean isDone();
    
    /**
     * Asks current task to perform one more processing iteration.
     * 
     * @return <code>true</code> if the processing is done; <code>false</code>
     *         otherwise
     */
    boolean iteration();
    
    /**
     * Asks current task to stop the processing (if any).
     */
    void stop();
}