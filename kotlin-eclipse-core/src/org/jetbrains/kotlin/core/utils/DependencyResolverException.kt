package org.jetbrains.kotlin.core.utils

import java.io.File

class DependencyResolverException(val resolvedFiles: List<File>) : RuntimeException()