package org.jetbrains.kotlin.preferences.views

interface Validable {
    val isValid: Boolean
    var onIsValidChanged: (Boolean) -> Unit
}