// FILE: Ann.java
import java.lang.annotation.Repeatable;

@Repeatable(Anns.class)
public @interface Ann {}

// FILE: Anns.java
public @interface Anns {
    Ann[] value();
}

// FILE: Aaa.kt
@Ann
class Aaa
