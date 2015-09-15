fun foo() {
   when (f<caret>1()) {
        true -> f2()
        else -> {
            f2()
        }
    }
}

fun f1() = true
fun f2() {}

fun main(args : Array<String>) {
    foo()
}

// LINE: 10