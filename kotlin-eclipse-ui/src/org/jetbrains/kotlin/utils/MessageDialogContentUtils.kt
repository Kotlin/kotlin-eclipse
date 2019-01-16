package org.jetbrains.kotlin.utils

import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.MultiStatus

object MessageDialogContentUtils {

    @JvmStatic
    fun getErrorTitleForMessageDialog(error: Throwable): String = error.localizedMessage

    @JvmStatic
    fun getErrorDescriptionForMessageDialog(error: Throwable): String = when(error) {
        is CoreException -> when (error.status) {
            is MultiStatus -> join(error.status.children.filterNot { it.isOK }.map { it.message }, "\n")
            else -> error.status.message
        }
        else -> error.localizedMessage
    }
}