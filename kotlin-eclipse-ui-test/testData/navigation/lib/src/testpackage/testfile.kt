package testpackage

public class SimpleClass {
    public fun <T> doNothing(arg: T): T = 
    		arg
    
    public fun <T> doNothingWithCollection(arg: Collection<T>): Collection<T> =
    		arg.map { doNothing(it) }
}

public class ComplicatedClass(val s: String) {
    public constructor(o: Any): this(o.toString())
}

public fun simpleFunInSingleFilePackage(): Unit = 
		println("I'm just a simple function")