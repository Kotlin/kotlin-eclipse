import testpackage.Misplaced
val c = Misplaced().<caret>simpleFunctionInMisplacedSourceClass() 
// SRC: misplaced-source-folder/simpleMisplaced.kt
// TARGET: (in testpackage.Misplaced).simpleFunctionInMisplacedSourceClass()