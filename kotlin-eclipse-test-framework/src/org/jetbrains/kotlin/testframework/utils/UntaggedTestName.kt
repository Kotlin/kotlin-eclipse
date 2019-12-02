package org.jetbrains.kotlin.testframework.utils

import org.junit.rules.TestWatcher
import org.junit.runner.Description

class UntaggedTestName: TestWatcher() {
    lateinit var methodName: String

    override fun starting(description: Description) {
        methodName = description.methodName.replace("""\[.*\]""".toRegex(), "").trim()
    }
}