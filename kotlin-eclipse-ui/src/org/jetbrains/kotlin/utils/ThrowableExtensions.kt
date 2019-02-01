package org.jetbrains.kotlin.utils

import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.MultiStatus

val Throwable.errorTitleForMessageDialog: String
    get() = localizedMessage

val Throwable.errorDescriptionForMessageDialog: String
    get() = when (this) {
        is CoreException -> when (status) {
            is MultiStatus -> join(status.children.filterNot { it.isOK }.map { it.message }, "\n")
            else -> status.message
        }
        else -> localizedMessage
    }