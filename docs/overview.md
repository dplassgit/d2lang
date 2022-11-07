# D2 Language Overview

D2 is a strongly-typed, statically-typed, inferred-type compiled language.
Its syntax draws from C, Java and Python.


## Data Types

The following types are built-in:

* `int`: 32 bit integer
* `byte`: 8 bit integer
* `float`: 64 bit floating point
* `bool`: boolean values (`true`, `false` are the two built-in values)
* `string`: immutable sequence of characters. There is no separate "character" type (as in Python)
* `record`: user-defined structure (akin to C `struct`)
* Arrays of any of the above types (except array)
* `null`: an un-set value that can be used in place of a string, record or array.

When defining an array, record or procedure, "forward references" (references 
before definitions) are allowed.


## Arithmetic and expressions

All arithmetic must be type-consistent. As of now there is no implicit (or
explicit) conversion between arithmetic types.

Similarly, all expressions must be type-consistent.


### Constants

Integer constants can represent -2^31-1 to 2^31. Example: 1234

Byte constants can represent -128 to +127, via a leading `0y` and hexadecimal. 
Example: `0y2f` for 47.

Floating constants must include a decimal point (dot). Example: 123.4


## Strings

String constants can be created with single or double quotes.


## Arrays

To define:

```
a: int[3] // declares & allocates an array in memory
a[0] = 1
a[1] = 2
```

Array literals:

```
lit = [1, 2, 3] // also allocates in memory
```

Use the `length` function to find the length of an array:

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
Records can be compared using `==` and `!=`. Two records
are `==` if every field is `==`, but not recursively. At this
time, D2's record comparison is "shallow".


## Variables

### Declaration

Variables do not need to be declared before assigned. You can 
still declare variables before use if you want:

```
a: int
b: string
c: r  // record type, either previously or subsequently defined
d: bool
```


### Assignment

If a variable is not declared, whatever type it is when first assigned will be its
type throughout its lifetime.

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

* `+`, `-`, `*`, `/`: for int, byte, float
* `%`: modulo for int, byte
* `|`: bitwise `or` for int, byte
* `&`: bitwise `and` for int, byte
* `^`: bitwise `xor` for int, byte
* `>>`: shift right for int, byte
* `<<`: shift left for int, byte

And all typical comparisons: `== != < <= > >=`.


### Boolean

* `AND` boolean and
* `OR` boolean or
* `NOT` boolean not
* `XOR` boolean xor

Booleans can be compared using `== != < <= > >=`. By definition, `true > false`.

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

To find the length of a string, use the built-in function `length`.

```
println length("abc") // prints 3 with newline
```


### Records

See above.


### Arrays

Use `[index]` to retrieve or set a single entry from an array.


## Statements

Statements can live inside or outside a procedure, like in Python.

Global statements are excuted top to bottom in a D2 file.


### Control structures

#### `if/elif/else`

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

Any number of `elif` clauses are allowed. Like Python (and unlike Java and C), 
parentheses are *not required* around the boolean expression.

There is no `case` statement in D2, like in Python.


#### `while` loop

```
while i < 10 do i = i + 1 {
  // loop statements
  println i
}
```

`break` and `continue` work the same as in Java, C, JavaScript, Python, etc.


### `print` and `println`

`print` prints a number, string or boolean variable to stdout. There is limited 
built-in support for printing arrays. There is no built-in support for printing
records.

`println variable` appends a newline after printing the object.


### `input`

Gets a string from stdin until EOF, or 1 MB of data is read. (D2 is still a toy language in some
ways.)

```
in: string
in = input
```

### Exit

`exit "Message"` prints the message on stdout and exits with return code -1.


### Procedures

#### Procedure definitions

This will define a procedure that takes two parameters: a string and an array of
strings, and returns an int:

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


NOTE: There is a limit of 4 parameters in procedure definitions and calls. There is 
an [open bug](https://github.com/dplassgit/d2lang/issues/63) to address this.


#### `main`

The procedure `main` is optional and if included, must be the last statement in a D2 file.

There is no support yet for passing argv/argc to `main`, but there is an 
[open bug](https://github.com/dplassgit/d2lang/issues/8) to finish this.

#### Externally defined procedures

Prepend `extern` before the `proc` keyword, and don't define the body of the procedure. Example:

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
