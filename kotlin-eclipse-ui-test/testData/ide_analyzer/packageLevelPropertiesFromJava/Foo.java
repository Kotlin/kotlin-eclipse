import kotlin.Function1;

public class Foo {
    void foo() {
        int x = _DefaultPackage.getX();
        Function1<Integer, Integer> inc = _DefaultPackage.getInc();
    }
}