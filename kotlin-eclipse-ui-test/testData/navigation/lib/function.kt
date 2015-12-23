import testpackage.SimpleClass
val x = SimpleClass().<caret>doNothing(42) 
// SRC: testpackage/testfile.kt
// TARGET: (in testpackage.SimpleClass).doNothing(T)