open class A (n: Int) {
}

class B: A(0) {
}

class C(): A(1)

fun test() {
    A()
    <caret>A(1)
}