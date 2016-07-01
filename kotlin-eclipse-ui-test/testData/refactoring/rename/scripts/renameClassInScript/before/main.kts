class TestKClass

fun sample(): TestKClass {
    val k: TestKClass = TestKClass()
    return k
}

TestKClass().apply {  }
with(TestKClass()) {
}