# Bootstrap v5

`v5.exe` (not checked in) is the output of the `v4fromv4.exe` compiler as run on the concatenation
of the source files in this directory. `v5.d` is generated by the bazel
target `:v5d` in the [BUILD file](BUILD) in this directory.

`v5fromv5.exe` (not checked in) is the output of the v5 compiler (`v5.exe`) as run
on `v5.d`.

## Features

`v5` supports everything that [v4 supports](../v4/README.md#features), plus:
   * Implemented with declarations

It does not yet support:
   * Arrays within records
   * `>>` `<<` operators
   * "Bit" operator `!`
   * `long`, `double`, `byte` data types