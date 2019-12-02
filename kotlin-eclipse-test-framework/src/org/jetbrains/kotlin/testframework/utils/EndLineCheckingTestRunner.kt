package org.jetbrains.kotlin.testframework.utils

import org.junit.runner.Runner
import org.junit.runners.BlockJUnit4ClassRunner
import org.junit.runners.Suite
import org.junit.runners.model.FrameworkMethod
import kotlin.reflect.full.isSubclassOf

class EndLineCheckingTestRunner(private val jclass: Class<*>) : Suite(null, emptyList()) {
    override fun getChildren(): List<Runner> =
        LineEnding.values().map { EndLineVariantTestRunner(jclass, it) }

    override fun getName() = "${jclass.name} [considering different line endings]"
}

private class EndLineVariantTestRunner(jclass: Class<*>, private val lineEnding: LineEnding) :
    BlockJUnit4ClassRunner(jclass) {
    override fun getName() = "[$lineEnding]"

    override fun testName(method: FrameworkMethod?) = "${super.testName(method)} $name"

    override fun collectInitializationErrors(errors: MutableList<Throwable>) {
        super.collectInitializationErrors(errors)
        if (!testClass.javaClass.kotlin.isSubclassOf(FileReaderHolder::class)) {
            errors.add(
                Exception(
                    "${testClass.javaClass.name} should implement ${FileReaderHolder::class.qualifiedName} " +
                            "in order to be used with ${EndLineCheckingTestRunner::class.qualifiedName}"
                )
            )
        }
    }

    override fun createTest() = (super.createTest() as FileReaderHolder).apply {
        val oldReader = fileReader
        fileReader = { oldReader(it).replace("""\r?\n""".toRegex(), lineEnding.replacement) }
    }
}


private enum class LineEnding(val replacement: String) {
    Windows("\r\n"),
    Unix("\n")
}