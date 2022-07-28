# How to run stuff

## Compiler

`./scripts/dcc foo.d` will compile into `d2out.exe` with optimizations.

`./scripts/dccrun foo.d` will compile into `d2out.exe` with optimizations, 
retain the temp files `foo.asm` and `foo.obj`, **and run the executable if
compilation passes**. It will remove `d2out.exe` before running.


## Interpreter

`./scripts/rund foo.d` will run `foo.d` using the interpreter and optimizations.


## A popular open-source compiler

`(compilername) foo.c` will produce `a.exe`

`(compilername) foo.c --save-temps -masm=intel` will produce `a.exe` and
`foo.s` in Intel format.
