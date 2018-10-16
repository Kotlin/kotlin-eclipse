package org.jetbrains.kotlin.core.utils

fun <T, S> pairOfNotNulls(first: T?, second: S?): Pair<T, S>? = second?.let { first?.to(it) }