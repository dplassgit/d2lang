# d2j v1

## What it does

Generates Java code from d2lang input. The intention is to be able to 
self-host on non-x64 architectures.

## Language Features

`v1j.d` supports:
   * Everything v0j.d supports, plus:
   * `DOUBLE`, `LONG`, arrays of all of the above
   * PRINTLN 
   * RECORDs
   * Bit operations `&` `|` `!` `^` (on integral types)
   * `>>` `<<` `++` `--` operators

It does not support:
   * Array literals
   * `extern`
   * `byte` data types

## To use

`D2Program.java` is the pre-built translation of `v1j.d` into Java, built
using v0j.d's .java output. You can use the Java version
to compile other `.d` programs into Java:

```
scripts/v1j samples/fib.d > fib.java
```

Then you can run the generated java code:

```
java fib.java`
```

NOTE: Not all d2lang features are supported (see above), so some of the samples
in the `samples` directory will not work.


## Bootstrapping

You can run the .java version of the compiler on itself:

```
java src/d2j/v1/D2Program.java < src/d2j/v1/v1j.d > v1j.java
```

The resulting .java file should be the same as D2Program.java.

