interface G<T> {
    fun foo(t : T) : T
}

class <caret>GC() : G<Int> {
    
}
