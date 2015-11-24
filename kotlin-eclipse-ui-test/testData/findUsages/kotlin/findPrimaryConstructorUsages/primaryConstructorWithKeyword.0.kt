open class A <caret>constructor(n: Int) {
}

class B: A(0) {
}

class C(): A(1)

fun test() {
    A()
    A(1)
}