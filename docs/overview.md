# D2 Language Overview

D2 is a strongly-typed, statically-typed, inferred-type compiled language.
Its syntax draws from C, Java and Python.

D2 keywords are **case insensitive**, but by tradition are written in UPPER
CASE. D2 variables are **case sensitive**.


## Data Types

The following types are built-in:

* `BOOL`: boolean values (`TRUE`, `FALSE` are the two built-in values)
* `BYTE`: 8-bit integer
* `INT`: 32-bit integer
* `LONG`: 64-bit integer
* `FLOAT`: 64-bit floating point
* `STRING`: immutable sequence of characters. There is no separate "character" type (as in Python)
* `RECORD`: user-defined structure (akin to C `struct`)
* Arrays of any of the above types (except array)
* `NULL`: an un-set value that can be used in place of a string, record or array.

When defining or using an array, record or procedure, "forward references" (reference
before definition) are allowed.


## Arithmetic and expressions

All arithmetic must be type-consistent. There is no implicit (or explicit) conversion between
arithmetic types, though you can use `gcc` functions to convert from FLOAT to INT (e.g., `round`.)

Similarly, all expressions must be type-consistent.


### Constants

Byte constants can represent -128 to +127, via a leading `0y` and hexadecimal.
Example: `0y2f` for 47 decimal.

Integer constants can represent -2^31-1 to 2^31. Example: `1234`

Long constants can represent -2^64-1 to 2^64. Example: `1234L`

Floating point constants **must** include a decimal point (dot). Example: `123.4`


## Strings

String constants can be created with single or double quotes. Empty string is permitted.
Strings can be added, and are immutable. 


## Arrays

To define:

```
a: int[3] // declares & allocates an array in memory
a[0] = 1
a[1] = 2
a[2] = 3
// print a[3] gives runtime exceptoin
```

Array literals:

```
lit = [1, 2, 3] // also allocates memory
```

Array literals can be used inline as well. 

Use the `LENGTH` function to find the length of an array:

```
x = length([1, 2, 3])
println x == 3
```

Arrays cannot be concatenated but they can be compared using
`==` and `!=`. Two arrays are `==` if every element is `==`.


## Records

To define:

```
r: record {
   f1: string
   f2: int
   f3: r  // "forward" reference
}
```

To instantiate a RECORD:

```
ar = new r
ar.f1 = 's'
ar.f2 = 3
ar.f3 = new r
```

Fields that are not set default to their default value (`0` for
numbers, `FALSE` for boolean, and `NULL` for others.)

Records can be compared using `==` and `!=`. Two records
are `==` if every field is `==`, but not recursively. At this
time, D2's record comparison is "shallow".


## Variables

### Declaration

Variables do not need to be declared before assignment. You can
still declare variables before use if you want:

```
a: int
b: string
c: r  // record type, either previously or subsequently defined
d: bool
```


### Assignment

Variable uses and assignments must match their declared types. If a variable is
not declared, whatever type is used when first assigned will be
its type throughout its lifetime.

```
a = 3
// if a==true // this is a compile-time error because int and bool cannot be compared.
```


### Globals

A global variable can be defined outside any procedure and can be of any type.


### Locals

A local variable can be declared or assigned inside a procedure. You cannot "shadow"
a global variable with a local variable of the same name.

```
a = 3 // global definition

f:proc {
  // a:bool // this is a compile-time error
}
```


## Operators

### Numbers

* `+`, `-`, `*`, `/`: for BYTE, INT, LONG, FLOAT
* `%`: modulo for BYTE, INT, LONG
* `|`: bitwise "or" for BYTE, INT, LONG
* `&`: bitwise "and" for BYTE, INT, LONG
* `^`: bitwise "xor" for BYTE, INT, LONG
* `>>`: shift right for BYTE, INT, LONG
* `<<`: shift left for BYTE, INT, LONG

And all typical comparisons: `== != < <= > >=`.


### Boolean

* `AND` boolean and
* `OR` boolean or
* `NOT` boolean not
* `XOR` boolean xor

Booleans can be compared using `== != < <= > >=`. By definition, `TRUE > FALSE`.


### Strings

Strings are immutable. Strings can be concatenated via the `+` operator, which
usually creates a new string in memory. Substrings (via `[index]`) may or may
not create a new string.

Examples:

```
s = 'string'
s2 = "another string"
s3 = s + s2
s4 = s3[0]
```

Strings can be compared using the usual operators `== != < <= > >=`.
Two strings are `==` if all their characters are `==`. Inequality (comparisons)
use the same semantics as C's `strcmp`.

Use `ASC` to get the ASCII int value of the first letter of a string, and `CHR` to
create a 1-character string from the ASCII int argument. (D2 does not support
Unicode yet.)

```
s = 'Ab'
a = asc(s) // 65
b = chr(a) // a one-character string 'A'
```

To find the length of a string, use the built-in function `LENGTH`.

```
println length("abc") // prints 3 with newline
```

### Records

See above.


### Arrays

Use `[index]` to retrieve or set a single entry from an array. Array indices must be `INT`s.


## Statements

Statements can live inside or outside a procedure, like in Python.

Global statements are excuted top to bottom in a D2 file.


### Control structures

#### `IF/ELIF/ELSE`

Typical Python semantics:

```
if i == 0 {  // must be a boolean expression
  println "zero"
} elif i == 1 {
  println "one"
} else {
  println "other"
}
```

Any number of `ELIF` clauses are allowed. Like Python (and unlike Java and C),
parentheses are *not required* around the boolean expression.

As in Python, there is no `CASE` or `SWITCH` statement in D2.


#### `WHILE`

```
i=0 while i < 10 do i++ {
  // loop statements
  println i
}
```

`BREAK` and `CONTINUE` work the same as in Java, C, JavaScript, Python, etc.


#### `EXIT`

Use `EXIT` to prematurely terminate the program. Its optional `STRING`
argument will be printed to stdout.


### `PRINT` and `PRINTLN`

`PRINT` prints a variable to stdout. There is limited built-in support for printing
arrays. There is no built-in support for printing records.

`PRINTLN expression` appends a newline after printing the value.


### `INPUT`

Reads a string from `stdin` until EOF, or 1 MB of data is read. (D2 is still a
toy language in some ways.)

```
in: string
in = input
```

### `EXIT`

`EXIT "Message"` prints the message on stdout and exits with return code -1.


### Command line arguments

Command-line arguments are provided in the built-in `ARGS` array (a 1-dimensional
array of `STRING`s.)


## Procedures

### Procedure definitions

This example defines a procedure that takes two parameters: a string and an
array of strings, and returns an int:

```
index: proc(arg1: string, arg2: string[]): int {
  return 0
}
```

Void procedures and no-arg procedures are supported:

```
think: proc { // parentheses are optional for no-arg proces
  // no return statement
}
```


### Procedure calls

Just like in C, Java, Python, JavaScript, etc.

```
x = index('hi', ['a', 'b', 'c'])
```

It is legal to ignore the return value of a procedure:

```
index('hi', ['a', 'b', 'c'])
```

### Externally-defined procedures

To reference a procedure in the `gcc` runtime library, prepend `EXTERN`
before the `PROC` keyword, and don't define the body of the
procedure. Example:

```
rand: extern proc(n: int): int
```

The procedure must be a procedure/function in the `gcc` runtime library.


## Runtime Checks

There are runtime checks in place for
* `NULL` pointer dereferences (`STRING`s, `RECORD`s, arrays)
* index out of range for `STRING` and array references
* division by zero and modulo zero

When possible, the compiler will discover these at compile time.
