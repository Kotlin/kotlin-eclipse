class Some {
	fun filter(predicate : (T) -> Boolean) = throw UnsupportedOperationException()
}

fun main(args: Array<String>) {
    Some().fil<caret>
}