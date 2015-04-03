fun someFun(f: () -> Unit) {
	f()
}

fun main(args : Array<String>) {
	someF<caret> {  }
}