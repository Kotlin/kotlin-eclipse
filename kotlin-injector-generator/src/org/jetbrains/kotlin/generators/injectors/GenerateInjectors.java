package org.jetbrains.kotlin.generators.injectors;

import java.io.IOException;

public class GenerateInjectors {
    public static void main(String[] args) throws IOException {
        InjectorsGenerator.generatorForTopDownAnalyzerForJvm().generate();
    }
}
