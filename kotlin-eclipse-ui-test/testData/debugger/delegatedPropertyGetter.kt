fun foo() {
    a<caret>a
}

val aa by Delegate()

class Delegate {
    fun get(t: Any?, p: PropertyMetadata) = 1
}

fun main(args : Array<String>) {
    foo()
}

// LINE: 8