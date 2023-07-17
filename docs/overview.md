# D2 Language Overview

D2 is a strongly-typed, statically-typed, inferred-type compiled language.
Its syntax draws from C, Java and Python.

D2 keywords are **case insensitive**, but by tradition are written in UPPER
CASE. D2 variables are **case sensitive**.


## Data Types

The following types are built-in:

* `INT`: 32-bit integer
* `BYTE`: 8-bit integer
* `LONG`: 64-bit integer
* `FLOAT`: 64-bit floating point
* `BOOL`: boolean values (`TRUE`, `FALSE` are the two built-in values)
* `STRING`: immutable sequence of characters. There is no separate "character" type (as in Python)
* `RECORD`: user-defined structure (akin to C `struct`)
* Arrays of any of the above types (except array)
* `NULL`: an un-set value that can be used in place of a string, record or array.

When defining or using an array, record or procedure, "forward references" (reference
before definition) are allowed.


## Arithmetic and expressions

All arithmetic must be type-consistent. As of now there is no implicit (or
explicit) conversion between arithmetic types, though you can use `gcc` functions
to convert from float to int (e.g., `round`.)

Similarly, all expressions must be type-consistent.

### Constants

Integer constants can represent -2^31-1 to 2^31. Example: `1234`

Long constants can represent -2^64-1 to 2^64. Example: `1234L`

Byte constants can represent -128 to +127, via a leading `0y` and hexadecimal.
Example: `0y2f` for 47 decimal.

Floating point constants **must** include a decimal point (dot). Example: `123.4`


## Strings

String constants can be created with single or double quotes.


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

To create:

```
ar = new r
ar.f1 = 's'
ar.f2 = 3
ar.f3 = new r
```

Fields that are not set default to their default value (zero for
numbers, false for boolean, and `NULL` for others.)

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

If a variable is not declared, whatever type it is when first assigned will be
its type throughout its lifetime.

```
a = 3
// if a==true // this is a compile-time error because int and bool cannot be compared.
```


### Globals

A global variable can be defined outside any procedure and can be of any type.


### Locals

A local variable can be declared or assigned inside a procedure. You cannot "shadow"
a global variable with a local variable of the same name

```
a = 3 // global definition

f:proc {
  // a:bool // this is a compile-time error
}
```


## Operators

### Numbers

* `+`, `-`, `*`, `/`: for INT, BYTE, FLOAT, LONG
* `%`: modulo for INT, BYTE, LONG
* `|`: bitwise "or" for INT, BYTE, LONG
* `&`: bitwise "and" for INT, BYTE, LONG
* `^`: bitwise "xor" for INT, BYTE, LONG
* `>>`: shift right for INT, BYTE, LONG
* `<<`: shift left for INT, BYTE, LONG

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

As in Python, there is no `case` statement in D2.


#### `WHILE` loop

```
while i < 10 do i = i + 1 {
  // loop statements
  println i
}
```

`BREAK` and `CONTINUE` work the same as in Java, C, JavaScript, Python, etc.


### `PRINT` and `PRINTLN`

`PRINT` prints a variable to stdout. There is limited built-in support for printing
arrays. There is no built-in support for printing records.

`PRINTLN variable` appends a newline after printing the object.


### `INPUT`

Reads a string from `stdin` until EOF, or 1 MB of data is read. (D2 is still a
toy language in some ways.)

```
in: string
in = input
```

### `EXIT`

`EXIT "Message"` prints the message on stdout and exits with return code -1.


### Procedures

#### Procedure definitions

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


#### Procedure calls

Just like in C, Java, Python, JavaScript, etc.

```
x = index('hi', ['a', 'b', 'c'])
```

It is legal to ignore the return value of a procedure:

```
index('hi', ['a', 'b', 'c'])
```

#### Command line arguments

Command-line arguments are provided in the built-in `ARGS` array (a 1-dimensional
array of `STRING`s.)

#### Externally defined procedures

Prepend `EXTERN` before the `PROC` keyword, and don't define the body of the
procedure. Example:

```
rand: extern proc(n: int): int
```

This must be a procedure/function in the `gcc` runtime library.


## Runtime Checks

There are runtime checks in place for
* null pointer dereferences (strings, records, arrays)
* index out of range for string and array references
* division by zero and modulo zero

When possible, the optimizer will find division by zero at compile time.
