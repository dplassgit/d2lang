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
* `null`: Un-set value that can be used in place of a string, record or array.


When defining an array, record or procedure, 'forward' references are allowed.


## Arithmetic

All arithmetic must be type-consistent. As of now there is no implicit (or explicit) conversion between
arithmetic types.

The following operators are available:



### Constants

Integer constants can represent -2^31-1 to 2^31. Example: 1234

Byte constants can represent -128 to +127, via a leading `0y` and hexadecimal. 
Example: `0y2f` for 47.

Floating constants must include a decimal point (dot). Example: 123.4


## Strings

Strings are immutable. Strings can be concatenated via the `+` operator, which
usually creates a new string. Substrings may or may not create a new string.
To find the length of a string, use the built-in function `length`.

String constants can be indicated with single or double quotes.


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


### Strings

Use `+` to concatenate strings. Use `[index]` to retrieve a single-character substring
of a string.


### Records

See above.

### Arrays

Use `[index]` to retrieve or set a single entry from an array


## Statements

Statements can live inside or outside a procedure (like in Python.)

Global statements are excuted top to bottom in a D2 file.


### Control structures

#### If/Else

#### While

```
while i< 10 do i = i + 1 {
  // loop statements
}
```

### Print

### Input

### Exit

### Procedures

#### Procedure definitions

Define a procedure that takes a string and an array of string, that returns an int:

```
index: proc(arg1: string, arg2: string[]): int {
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



#### main


#### Externally defined procedures


## Runtime
