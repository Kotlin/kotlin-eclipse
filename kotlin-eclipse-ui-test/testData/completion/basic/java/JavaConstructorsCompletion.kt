package sdf.sdf

import java.util.HashMap

fun main(args : Array<String>) {
	val map = HashMap<Int, Int>()
	map.<caret>
}

// EXIST: keySet, get
// Check that there is no exception 