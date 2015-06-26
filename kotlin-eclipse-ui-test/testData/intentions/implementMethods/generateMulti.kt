package something

interface Some<T> {
    fun someFoo()
    fun someOtherFoo() : Int
    fun someGenericFoo() : T
}

class<caret> SomeOther<S> : Some<S> {
    
}