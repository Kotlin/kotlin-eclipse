// From KT-1254
interface T {
    fun Foo() : (String) -> Unit
}

class <caret>C : T {
  
}