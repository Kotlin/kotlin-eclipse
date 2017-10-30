package org.jetbrains.kotlin.ui.commands.j2k

fun prettify(code: String?): String {
    if (code == null) {
        return ""
    }

    return code
            .trim()
            .replace("\r\n", "\n")
            .replace(" \n", "\n")
            .replace("\n ", "\n")
            .replace("\n+".toRegex(), "\n")
            .replace(" +".toRegex(), " ")
            .trim()
}