val SomeVal4 = 4
val someVal1 = 1
val SomeVal3 = 3
val someVal2 = 2

fun test() {
    some<caret>
}

// ORDER: someVal1
// ORDER: someVal2
// ORDER: SomeVal3
// ORDER: SomeVal4
