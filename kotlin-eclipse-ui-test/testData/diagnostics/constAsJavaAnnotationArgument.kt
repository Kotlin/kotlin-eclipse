// FILE: Consts.java
public interface Consts {
	short value = 5
}

// FILE: Ann.java
public @interface Ann {
	short value();
}

// FILE: Aaa.kt
@Ann(Consts.value)
class Aaa
