# How to run stuff

## Compiler

`./scripts/dcc foo.d` will compile into `d2out.exe` with optimizations.

`./scripts/dccrun foo.d` will compile into `d2out.exe` with optimizations, 
retain the temp files `foo.asm` and `foo.obj`, **and run the executable if
compilation passes**. It will remove `d2out.exe` before running.


## Interpreter

`./scripts/rund foo.d` will run `foo.d` using the interpreter and optimizations.


## Nasm

Assume `osc` is the open source compiler.

`nasm -fwin64 -Ox foo.asm && osc foo.obj && ./a.exe`


## A popular open-source compiler

To compile a random c file:

Assume `osc` is the open source compiler.

`osc foo.c` will produce `a.exe`

`osc foo.c --save-temps -masm=intel` will produce `a.exe` and
`foo.s` in Intel format.
