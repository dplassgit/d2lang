# d2lang

D2 is a strongly-typed, statically-typed, inferred-type compiled language. 
Its syntax draws from C, Java and Python.

The D2 compiler compiles to X64 assembly language only. It currently uses
`nasm` and `gcc` to assemble and link, respectively, to Windows executables only.

See the [overview](docs/overview.md) for a more comprehensive description of the 
types, control structures, operators and statements in D2.

<tt>A <a href="http://www.plasstech.com/a-plass-program">PLASS</a> Program</tt>


## Installing

1. Install Eclipse or [bazel](https://github.com/bazelbuild/bazel).

2. Install [nasm](https://www.nasm.us/)

3. Install [gcc](https://gcc.gnu.org/install/binaries.html)

4. Optional: Install git bash shell (mingw64)


## Running Tests

`bazel test ...`

See also [docs/running.md](docs/running.md)


## Caveats

Only compiles to Intel x64. Only links the Windows version of the `gcc` runtime 
library. Only uses `nasm` and `gcc`. 

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

printPeg: proc(peg:int) {
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
