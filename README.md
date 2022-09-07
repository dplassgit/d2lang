# d2lang

A simple type-inferred language compiler.

<tt>A <a href="http://www.plasstech.com/a-plass-program">PLASS</a> Program</tt>

## Installing

1. Install Eclipse or [bazel](https://github.com/bazelbuild/bazel).

2. Install [nasm](https://www.nasm.us/)

3. Install [gcc](https://gcc.gnu.org/install/binaries.html)

4. Optional: Install git bash shell (mingw64.)


## Running Tests

`bazel test ...`

See also docs/running.md


## Caveats

Only compiles to Intel x64. Only links the Windows version of the gcc runtime 
library. Only uses `nasm` and `gcc`.


## Getting the source code

`git clone git@github.com:dplassgit/d2lang.git` clones into the `d2lang` 
subdirectory.

`git pull`

`git push origin trunk`

Using from git bash shell should let us log in from the browser.

