interface T {
    fun foo()
    fun bar()
}

class<caret> C(t :T) : T by t {
    
}

// KT-5103