package org.jetbrains.kotlin.generators.injectors

import org.jetbrains.kotlin.generators.di.DependencyInjectorGenerator
import org.jetbrains.kotlin.generators.di.Expression
import org.jetbrains.kotlin.generators.di.DiType
import org.jetbrains.kotlin.generators.di.InjectorGeneratorUtil

// Copied from org.jetbrains.kotlin.generators.di.InjectorGeneratorFacade
public fun generator(
        targetSourceRoot: String,
        injectorPackageName: String,
        injectorClassName: String,
        generatedBy: String,
        body: DependencyInjectorGenerator.() -> Unit
): DependencyInjectorGenerator {
    val generator = DependencyInjectorGenerator()
    generator.configure(targetSourceRoot, injectorPackageName, injectorClassName, generatedBy)
    generator.body()
    return generator
}

inline public fun <reified T> DependencyInjectorGenerator.field(
        name: String = defaultName(javaClass<T>()),
        init: Expression? = null,
        useAsContext: Boolean = false
) {
    addField(false, DiType(javaClass<T>()), name, init, useAsContext)
}

inline public fun <reified T> DependencyInjectorGenerator.publicField(
        name: String = defaultName(javaClass<T>()),
        init: Expression? = null,
        useAsContext: Boolean = false
) {
    addField(true, DiType(javaClass<T>()), name, init, useAsContext)
}

inline public fun <reified T> DependencyInjectorGenerator.parameter(
        name: String = defaultName(javaClass<T>()),
        useAsContext: Boolean = false
) {
    addParameter(false, DiType(javaClass<T>()), name, true, useAsContext)
}

inline public fun <reified T> DependencyInjectorGenerator.publicParameter(
        name: String = defaultName(javaClass<T>()),
        useAsContext: Boolean = false
) {
    addParameter(true, DiType(javaClass<T>()), name, true, useAsContext)
}

public fun defaultName(entityType: Class<*>): String = InjectorGeneratorUtil.`var`(DiType(entityType))