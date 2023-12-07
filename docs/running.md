# How to run the D2 compiler

## Prerequisites

To compile the D2 compiler, the following are needed:

1. [bazel](https://github.com/bazelbuild/bazel)

2. [nasm](https://www.nasm.us/)

3. [gcc](https://gcc.gnu.org/install/binaries.html)

4. Java 11 or higher

Note: the D2 compiler can *only* generate X64 Assembly language and link against
the Windows C Standard Library.

## Compiling

`./scripts/dcc foo.d` will compile the compiler, then use it to compile `foo.d`
into `d2out.exe` with optimizations.

`./scripts/dccrun foo.d` will compile the compiler, then use it to compile `foo.d` into
`d2out.exe` with optimizations, retain the temp files `foo.asm` and `foo.obj`, and run the
executable **if compilation passes**.

### Command-line flags

These flags can be passed to `dcc` or `dccrun`:

```
  --[no]compileAndAssembleOnly [-c] (a boolean; default: "false")
    Compile and assemble; do not link (generates .asm and .obj).
  --[no]compileOnly [-S] (a boolean; default: "false")
    Compile only; do not assemble or link (generates .asm).
  --debugcodegen (an integer; default: "0")
    Sets debug level for code generator.
  --debugint (an integer; default: "0")
    Sets debug level for interpreter.
  --debuglex (an integer; default: "0")
    Sets debug level for lexer.
  --debugopt (an integer; default: "0")
    Sets debug level for optimizer.
  --debugparse (an integer; default: "0")
    Sets debug level for parser.
  --debugtype (an integer; default: "0")
    Sets debug level for type checker.
  --exe [-o] (a string; default: "d2out.exe")
    Sets the executable name.
  --[no]help [-h] (a boolean; default: "false")
    Prints usage info.
  --libs [-l] (comma-separated list of options; may be used multiple times)
    Libraries to link
  --[no]optimize (a boolean; default: "true")
    Turns on the optimizer.
  --[no]save-temps (a boolean; default: "false")
    If the asm and obj files should be kept.
  --[no]show-commands [-v] (a boolean; default: "false")
    Show intermediate commands
  --[no]show-stack-traces [-T] (a boolean; default: "false")
    Shows full stack trace of compile-time errors
  --target (x64 or t100; default: "x64")
    Target architecture
```

### Environment variables

Set `D2PATH` to the root of the compiler and you will be able to call procedures
in [dlib](../dlib/README.md) (as `extern`s.)

## Interpreter

`./scripts/rund foo.d` will run `foo.d` using the interpreter and optimizations.

**NOTE**: the interpreter does not support `extern`s or `dlib`.
