# Bootstrap v4

`v4.exe` (not checked in) is the output of the `v3fromv3.exe` compiler as run on the concatenation
of the source files in this directory. `v4.d` is generated by the bazel
target `:v4d` in the [BUILD file](BUILD) in this directory.

`v4fromv4.exe` (not checked in) is the output of the v4 compiler (`v4.exe`) as run
on `v4.d`.

## Features

`v4` supports everything that [v3 supports](../v3/README.md#features), plus:
   * Unlimited number of locals
   * Implemented with array literals, `++`, `--`, `extern`s
   * "Bit" operators (`&`, `|`, `^`)
   * Boolean `xor`
   * Ints are stored as 32-bits only, and bools as 8 bits only
   * `Extern` bugfix
   * Keywords are case-insensitive
   * `String`s can be compared to `null`
   * Declarations

It does not yet support:
   * Arrays within records
   * `>>` `<<` operators
   * "Bit" operator `!`
   * `long`, `double`, `byte` data types