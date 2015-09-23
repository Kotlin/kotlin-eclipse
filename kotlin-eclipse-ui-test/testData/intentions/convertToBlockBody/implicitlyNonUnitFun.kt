interface Item {
    fun title(): String
    fun wight(): Int
}

class Book: Item {
    override fun <caret>title() = "Book"
    override fun wight() = 12
}