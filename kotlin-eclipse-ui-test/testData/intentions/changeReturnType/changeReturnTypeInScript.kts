fun testReturn(): List<Int> {
    return listOf(1, 2).toType<caret>dArray()
}