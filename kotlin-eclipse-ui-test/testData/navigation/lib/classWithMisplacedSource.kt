import testpackage.Misplaced
val x: <caret>Misplaced? = null 
// SRC: misplaced-source-folder/simpleMisplaced.kt
// TARGET: (testpackage).Misplaced