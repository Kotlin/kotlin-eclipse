fun foo() {
    f2("aaa${f<caret>1()}")
}

fun f1() = "1"
fun f2(s: String) {}

fun main(args : Array<String>) {
    foo()
}

// LINE: 5