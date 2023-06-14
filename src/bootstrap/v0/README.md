# Bootstrap v0

`v0.exe` is the output of the Java-written compiler as run on `v0.d`.

`v0fromv0.exe` is the output of the d2-written compiler (`v0.exe`) as run
on `v0.d`.

They produce identical output!

## Features

`v0.d` supports:
   * 100 procedures with a maximum of 4 parameters each and 10 locals
   * `int`, `bool`, `string`, arrays of `int`, `bool`, `string`

It does not support:
   * Array literals
   * `records`
   * Comparing strings via `<` or `>`
   * `>>` `<<` `++` `--` operators
   * `extern`
   * "Bit" operators (`&`, `|`, `!`, `^`)
   * `long`, `double`, `byte` data types
