fun foo(): Int {
    val greet = "hello"
    return with(greet) {
        return@with greet<caret>
    }
}