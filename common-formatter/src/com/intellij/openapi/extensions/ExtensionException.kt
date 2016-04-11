package com.intellij.openapi.extensions

class ExtensionException(val extensionClass: Class<*>) : RuntimeException(extensionClass.getCanonicalName())