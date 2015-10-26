package kotlin.testing

import testing.NewInterface

class Some(s: NewInterface) : NewInterface() {
    val test = s

    fun testFun(param : NewInterface) : NewInterface {
        return test;
    }
}