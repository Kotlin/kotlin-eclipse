class SomeClass {
  class SomeInternal

  fun some(a : S<caret>)
}

// INVOCATION_COUNT: 2
// EXIST: SomeClass
// EXIST: SomeInternal
// EXIST: String 
// EXIST: StringBuilder
// EXIST_JAVA_ONLY: StringBuffer
// EXIST_JS_ONLY: HTMLStyleElement
// EXIST_JAVA_ONLY: { lookupString:"Statement", tailText:" (java.sql)" }

// TODO: json { lookupString:"String", tailText:" (jet)" }