open class Bar : Throwable()

val foo: Bar = Bar()
val foo1: Bar = Bar()

val foos: List<Bar> = listOf()
val foos1: Array<Bar> = array()

fun main(args: Array<String>) {
    val foo: Bar = Bar()
    val someVerySpecialFoo: Bar = Bar()
    val fooAnother: Bar = Bar()

    val anonymous = object : Bar() {
    }

    val (foo1: Bar, foos: List<Bar>) = Pair(Bar(), listOf<Bar>())

    try {
        for (foo2: Bar in listOf<Bar>()) {

        }
    } catch (foo: Bar) {

    }

    fun local(foo: Bar) {

    }
}

fun topLevel(foo: Bar) {

}

fun collectionLikes(foos: List<Array<Bar>>, foos: List<Map<Bar, Bar>>) {

}

class FooImpl : Bar()

object FooObj : Bar()