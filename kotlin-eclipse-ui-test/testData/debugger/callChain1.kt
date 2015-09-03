fun foo() {
    A().ge<caret>tB().f1()
}

class A {
    fun getB() = B()
}

class B {
    fun f1() {}
}

fun main(args : Array<String>) {
    //Breakpoint!
    foo()
}

// LINE: 6