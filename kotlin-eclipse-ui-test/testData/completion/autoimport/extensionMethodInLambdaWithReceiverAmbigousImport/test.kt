fun main() {
    with("") {
        fo<caret>o(27)
    }
}

// NUMBER: 2
// EXIST: Import 'foo' (dependencies1)
// EXIST: Import 'foo' (dependencies2)
