package org.jetbrains.kotlin.core.asJava;

import org.jetbrains.org.objectweb.asm.ClassWriter;

public class BinaryClassWriter extends ClassWriter {
    public BinaryClassWriter() {
        super(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        try {
            return super.getCommonSuperClass(type1, type2);
        }
        catch (Throwable t) {
            // TODO: we might need at some point do more sophisticated handling
            return "java/lang/Object";
        }
    }
}