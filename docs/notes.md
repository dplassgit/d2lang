# Notes

## Influences/design philosophy

D2 has some similarities to C (& C++ & Java & JavaScript) in that it uses
curly braces, and it currently has no OO/classes. It has some similarities
to Java in that strings are immutable (as are array sizes). It has some
similarities to Python in that there is no “case” statement, there are no
semicolons, and there is no “char” type. Finally, it has some similarities to
JavaScript in that you can use single or double quotes around strings.

Some differences from C, Java, Python, JavaScript:
   * Curly braces are REQUIRED to start all blocks
   * You can’t start a block in the middle of another block
   * No parentheses are required for if/while expressions
   * There is no casting between types.

I tried to enforce a pattern of `variablename: type`, and so even procedure
signatures look like that:

```
fact: proc(n: int): int {
  if n <= 1 { return n }
  else { return n * fact(n - 1) }
}
```

## Top-level design

The compiler is split into phases; each phase updates the current state of
the world:
   * Parser: given an input program, produces a parse tree. It’s a recursive-descent parser that uses the Lexer on demand. (The Lexer is not a phase).
   * Type checker: Given a parse tree, creates a symbol table with globals, procs and record definitions. Checks for type correctness and infers types when they are not set
   * Intermediate language (IL) generator: Given a parse tree and symbol table, generates IL code that is not D-specific
   * IL Optimizers: Given an IL program, optimizes it in various ways:
   * Arithmetic optimization (e.g., 1+1 optimizes to 2, a\*0 optimizes to 0)
      * Dead code elimination
      * Others
   * Nasm code generator: Given an IL program (optimized or not), generates assembly language

There is also a compiler “driver” that calls all the above phases, and then
(optionally) nasm to assemble and (optionally) gcc to link into an executable.

Right now there is no way to combine D2 files, but there are multiple open bugs.

