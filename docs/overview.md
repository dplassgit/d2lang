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
before definition) are allowed.


## Arithmetic

All arithmetic must be type-consistent. As of now there is no implicit (or
explicit) conversion between arithmetic types.

The following operators are available:



### Constants

Integer constants can represent -2^31-1 to 2^31. Example: 1234

Byte constants can represent -128 to +127, via a leading `0y` and hexadecimal. 
Example: `0y2f` for 47.

Floating constants must include a decimal point (dot). Example: 123.4


## Strings

Strings are immutable. Strings can be concatenated via the `+` operator, which
usually creates a new string. Substrings may or may not create a new string.

String constants can be indicated with single or double quotes.

Examples:

```
s = 'string'
s2 = "another string"
s3 = s + s2
s4 = s3[0]
```


## Arrays

To define:

```
a:int[3]
a[0] = 1
a[1] = 2
```

Array literals:

```
lit=[1, 2, 3]
```

Use the `length` function to find the length of an array.

Arrays cannot be concatenated.


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
ar=new r
ar.f1 = 's'
ar.f2 = 3
ar.f3 = new r
```


## Variables

### Declaration

Local and global variables need not be declared before assigned. However, you can 
still declare variables before use:

```
a:int
b:string
c:r  // record type, previously defined
d:bool
```


### Assignment

If a variable is not declared, whatever type it is when first assigned will be its
type throughout its lifetime.


### Globals

A global variable can be defined outside any procedure.


### Locals

A local variable can be declared or assigned inside a procedure. You cannot "shadow"
a global variable with a local variable of th esame name.


## Operators

### Numbers

* `+`: for int, byte, float
* `-`, `*`, `/`: for int, byte, float
* `%`: modulo for int, byte
* '|': bitwise or for int, byte
* '&': bitwise and for int, byte
* '^': bitwise xor for int, byte
* '>>': shift right for int, byte
* '<<': shift left for int, byte

And all comparisons: `== != < <= > >=`.


### Boolean

* `AND` boolean and
* `OR` boolean or
* `NOT` boolean not
* `XOR` boolean xor


### Strings

Use `+` to concatenate strings. Use `s[index]` to extract a single-character 
substring of a string.

Use `ASC` to convert from the first letter of a string to int, and `CHR` for
int to a 1-character string.

To find the length of a string, use the built-in function `length`.


### Records

See above.


### Arrays

Use `[index]` to retrieve or set a single entry from an array


## Statements

Statements can live inside or outside a procedure (like in Python.)

Global statements are excuted top to bottom in a D2 file.


### Control structures

#### `if/elif/else`


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

#### `while` loop

```
while i< 10 do i = i + 1 {
  // loop statements
  break
}
```

`break` and `continue` work the same as in Java, C, JavaScript, Python, elt.


### `print` and `println`

`print` prints a number, string or boolean variable to stdout. Limited support
for printing arrays. No support for printing records at this time.

`println variable` appends a newline to printing the object.


### `input`

Gets a string from stdin until EOF, or 1 MB. (D2 is still a toy language in some
ways.)

```
in: string
in = input
```

### Exit

`exit "Message"` prints the message on stdout and exits with return code -1.


### Procedures

#### Procedure definitions

This declaration will define a procedure that takes a string and an array of
strings, and returns an int:

```
index: proc(arg1: string, arg2: string[]): int {
   return 0
}
```

Void procedures are supported:

```
think: proc { // parentheses are optional for no-arg proces
  // no return statement
}
```


#### Procedure calls

Just like in C, Java, Python:

```
x = index('hi', ['a', 'b', 'c'])
```

It is legal to ignore the return value of a procedure:

```
index('hi', ['a', 'b', 'c'])
```


NOTE: There is a limit of 4 parameters in procedure definitions and calls. There is 
an [open bug](issues/63) to address this.


#### main

The procedure `main` is optional and if included, must be the last 'statement' in a D2 file.

There is no support for passing argv/argc to `main`, but there is an [open bug](issues/8) to finish this.

#### Externally defined procedures

Prepend `extern` before the `proc` keyword. This must be a procedure/function in the gcc
runtime library.

```
rand: extern proc(n: int): int
```


## Runtime Checks

There are runtime checks in place for
* null pointer dereferences (strings, records, arrays), 
* index out of range for strings and arrays
* division by zero (and modulo zero)

When possible, the optimizer will find division by zero at compile time.


