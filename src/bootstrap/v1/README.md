# Bootstrap v1

`v1.exe` (not checked in) is the output of the `v0.exe` compiler as run on 
the concatenation of the source files in this directory. `v1.d` is generated
by the bazel target `:v1d` in the [BUILD file](BUILD) file in this directory.

`v1fromv1.exe` is the output of the v1-written compiler (`v1.exe`) as run
on `v1.d`.

They produce identical output!

## Features

`v1` supports everything that [v0 supports](../v0/README.md#features), plus:
   * Up to 20 `record`s (`new`, field references, `null` comparisons)
   * `println`
   * More type-checking, including null-pointer and negative index checks.
   * Better error messages, including line number
   * Comparing booleans, `not`

It does not support:
   * Unlimited number of locals
   * Unlimited number of records
   * Array literals
   * Arrays within records
   * Comparing strings via `<` or `>`
   * `>>` `<<` `++` `--` operators
   * `extern`
   * "Bit" operators (`&`, `|`, `!`, `^`)
   * `long`, `double`, `byte` data types
