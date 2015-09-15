fun foo() {
    A().getB().f<caret>1()
}

class A {
    fun getB() = B()
}

class B {
    fun f1() {}
}

fun main(args : Array<String>) {
    foo()
}

// LINE: 10