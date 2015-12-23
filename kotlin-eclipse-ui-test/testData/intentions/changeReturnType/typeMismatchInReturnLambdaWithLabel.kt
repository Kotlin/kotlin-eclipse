fun foo(): Int {
    val greet = "hello"
    return with(greet) {<caret>
        return@with greet
    }
}