package first

fun firstFun() {
  val a = In<caret>
}

// EXIST: Int
// INVOCATION_COUNT: 0

// TODO absent: JSon { lookupString:"Int", tailText:" (jet.runtime.SharedVar)" }
// TODO exist: JSon { lookupString:"Int", tailText:" (jet)" }