fun main(args : Array<String>) {
	val list = Enum<caret>Map
	println(list)
}

// EXIST: Import 'EnumMap' (java.util)
// NUMBER: 1