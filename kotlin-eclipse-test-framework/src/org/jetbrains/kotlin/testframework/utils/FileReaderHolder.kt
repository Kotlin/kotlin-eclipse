package org.jetbrains.kotlin.testframework.utils

interface FileReaderHolder {
    var fileReader: (String) -> String

    companion object {
        operator fun invoke() = object: FileReaderHolder {

            override var fileReader: (String) -> String = KotlinTestUtils::getText
        }
    }
}