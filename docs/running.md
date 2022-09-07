# How to run stuff

## Compiler

`./scripts/dcc foo.d` will compile `foo.d` into `d2out.exe` with optimizations.

`./scripts/dccrun foo.d` will compile `foo.d` into `d2out.exe` with optimizations, 
retain the temp files `foo.asm` and `foo.obj`, **and run the executable if
compilation passes**. It will remove `d2out.exe` before running.


## Interpreter

`./scripts/rund foo.d` will run `foo.d` using the interpreter and optimizations.
The interpreter does not support externs.


## Nasm

`nasm -fwin64 foo.asm` generates `foo.obj`

To run the whole chain and generate `foo.exe`:

`nasm -fwin64 foo.asm && gcc foo.obj -o foo.exe` 


## GCC

To compile a random c file:

`gcc foo.c -o foo.exe` will produce `foo.exe`

`gcc foo.c --save-temps -masm=intel` will produce `a.exe` and `foo.s` in Intel
format.
