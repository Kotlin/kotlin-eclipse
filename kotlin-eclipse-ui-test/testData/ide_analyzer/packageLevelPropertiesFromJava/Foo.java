import kotlin.jvm.functions.Function1;

public class Foo {
    void foo() {
        int x = BarKt.getX();
        Function1<Integer, Integer> inc = BarKt.getInc();
    }
}