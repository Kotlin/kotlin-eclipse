package org.jetbrains.kotlin.core.utils

import org.jetbrains.kotlin.name.FqName

public fun FqName.tail(prefix: FqName): FqName {
    return when {
        !isSubpackageOf(prefix) || prefix.isRoot() -> this
        this == prefix -> FqName.ROOT
        else -> createFqName(asString().substring(prefix.asString().length() + 1))
    }
}

public fun FqName.isSubpackageOf(packageName: FqName): Boolean {
    return when {
        this == packageName -> true
        packageName.isRoot() -> true
        else -> isSubpackageOf(this.asString(), packageName.asString())
    }
}

private fun isSubpackageOf(subpackageNameStr: String, packageNameStr: String): Boolean {
	return subpackageNameStr.startsWith(packageNameStr) && subpackageNameStr[packageNameStr.length()] == '.'
}

private fun createFqName(str: String): FqName = FqName(str) // TODO: bug, cannot create this type inside extension function
