import misplaced.first.ClassWithMisplacedIdenticalSource
val x: <caret>ClassWithMisplacedIdenticalSource? = null 
// SRC: misplaced-source-folder/first/misplaced.kt
// TARGET: (misplaced.first).ClassWithMisplacedIdenticalSource