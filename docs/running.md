# How to run the D2 compiler

## Prerequisites

To compile the D2 compiler, the following are needed:

   * Java 11 or higher
   * Bazel

In order for the compiler to generate Windows executables, the following are also needed:
   * nasm
   * GCC

Note: the D2 compiler can *only* generate X64 Assembly language and link against
Windows (gcc) stdlib.

## Compiler

`./scripts/dcc foo.d` will compile `foo.d` into `d2out.exe` with optimizations.

`./scripts/dccrun foo.d` will compile `foo.d` into `d2out.exe` with optimizations,
retain the temp files `foo.asm` and `foo.obj`, **and run the executable if
compilation passes**. It will remove `d2out.exe` before running.


## Interpreter

`./scripts/rund foo.d` will run `foo.d` using the interpreter and optimizations.
**NOTE**: the interpreter does not support `extern`s.


## Nasm

`nasm -fwin64 foo.asm` generates `foo.obj`

To run the whole chain and generate `foo.exe`:

`nasm -fwin64 foo.asm && gcc foo.obj -o foo.exe`


## GCC

To compile a random c file:

`gcc foo.c -o foo.exe` will produce `foo.exe`

`gcc foo.c --save-temps -masm=intel` will produce `a.exe` and `foo.s` in Intel
format.
