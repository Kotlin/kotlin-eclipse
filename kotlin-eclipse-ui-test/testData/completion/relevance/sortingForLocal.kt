val someVal3 = 3
val someVal1 = 1
val someVal5 = 5
val someVal2 = 2
val someVal4 = 4

fun test() {
    someVal<caret>
}

// ORDER: someVal1
// ORDER: someVal2
// ORDER: someVal3
// ORDER: someVal4
// ORDER: someVal5