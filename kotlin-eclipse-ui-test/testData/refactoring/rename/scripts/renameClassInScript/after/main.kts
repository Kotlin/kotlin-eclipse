class RenamedKClass

fun sample(): RenamedKClass {
    val k: RenamedKClass = RenamedKClass()
    return k
}

RenamedKClass().apply {  }
with(RenamedKClass()) {
}