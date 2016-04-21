fun main(args : Array<String>) {
	val list = ArrayL<caret>ist<Int>()
	println(list)
}

// EXIST: Import 'ArrayList' (java.util)
// NUMBER: 1