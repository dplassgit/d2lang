# d2lang

D2 is a strongly-typed, statically-typed, inferred-type compiled language. 
Its syntax draws from C, Java and Python.

The D2 compiler currently compiles to X64 assembly language only. It uses
`nasm` and `gcc` to assemble and link, respectively, to Windows executables.

There are hooks to support other architectures; the intermediate language
is (mostly) target-agnostic and a 8085 backend is partially implemented.

See the [overview](docs/overview.md) for a more comprehensive description of the 
types, control structures, operators and statements in D2.

<tt>A <a href="http://www.plasstech.com/a-plass-program">PLASS</a> Program</tt>


## Installing

The following 4 are required:

1. [bazel](https://github.com/bazelbuild/bazel)

2. [nasm](https://www.nasm.us/)

3. [gcc](https://gcc.gnu.org/install/binaries.html)

4. Java 11 or higher

5. Optional: Eclipse and git bash shell (mingw64)


## Running Tests

Run `bazel test ...` from the root directory.


## Running the compiler

See [docs/running.md](docs/running.md).


## Caveats

Only compiles to Intel x64. Only links against the Windows version of the `gcc`
C Runtime Library. Can only use `nasm` and `gcc`. 

There are [various bugs](https://github.com/dplassgit/d2lang/labels/bug).


## Language sample

Canonical hello world:

```
println "Hello world"
```

[Tower of Hanoi](samples/hanoi.d)

```
// Ported from toy (http://www.graysage.com/cg/Compilers/Toy/hanoi.toy)

PEGS = ["", "left", "center", "right"]

printPeg: proc(peg: int) {
  print PEGS[peg]
}

hanoi: proc(n: int, fromPeg: int, usingPeg: int, toPeg: int) {
  if n != 0 {
    hanoi(n - 1, fromPeg, toPeg, usingPeg)
    print "Move disk from "
    printPeg(fromPeg)
    print " peg to "
    printPeg(toPeg)
    println " peg"
    hanoi(n - 1, usingPeg, fromPeg, toPeg)
  }
}

n = 5 // defines global
hanoi(n, 1, 2, 3)
```

See more [samples](samples)


## Why did I build D2?

See [docs/history](docs/history.md)
