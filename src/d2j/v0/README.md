# d2j v0

## What it does

Generates Java code from d2lang input. The intention is to be able to 
self-host on non-x64 architectures.

## Language Features

`v0j.d` supports:
   * 100 procedures with a maximum of 4 parameters each and 10 locals
   * `int`, `bool`, `string`, arrays of `int`, `bool`, `string`

It does not support:
   * `println`
   * Array literals
   * `record`s
   * Comparing strings via `<` or `>`
   * `>>` `<<` `++` `--` operators
   * `extern`
   * Bit operations `&` `|` `!` `^`
   * `byte`, `long`, or `double` data types

## To use

`D2Program.java` is the pre-built translation of `v0j.d` into Java, built
using v0j.d and the x64 compiler on Windows. You can use the Java version
to compile other `.d` programs into Java:

```
java src/d2j/v0/D2Program.java < samples/fib.d > fib.java
```

Then you can run the generated java code:

```
java fib.java`
```

NOTE: Not all d2lang features are supported (see above), so some of the samples
in the `samples` directory will not work.


## Bootstrapping

You can run the .java version of the compiler on the .d version of itself:

```
java src/d2j/v0/D2Program.java < src/d2j/v0/v0j.d > v0j.java
```

The resulting .java file should be the same as D2Program.java, except
with poorer formatting.

