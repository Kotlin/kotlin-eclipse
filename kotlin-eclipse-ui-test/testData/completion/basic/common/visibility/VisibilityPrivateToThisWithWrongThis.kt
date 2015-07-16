class A<in I> {
    private val bar: I

    private fun foo(): I = null!!


    fun test() {
        with(A()) {
            this.<caret>
        }
    }
}
public inline fun <T, R> with(receiver: T, f: T.() -> R): R = receiver.f()

// INVOCATION_COUNT: 1
// ABSENT: bar, foo
