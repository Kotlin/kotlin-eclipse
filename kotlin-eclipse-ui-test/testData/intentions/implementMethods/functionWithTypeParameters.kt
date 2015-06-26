interface Trait {
    fun <A, B : Runnable, E : Map.Entry<A, B>> foo() where B : Cloneable, B : Comparable<B>;
}

class <caret>TraitImpl : Trait {
    
}