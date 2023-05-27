# The D(2) Language Features

## Types & Keywords

Built-in types: int, string, boolean, float, byte, arrays, records

Eventually: long, n-dimensional arrays, map, lambda (but not closures)

Current keywords (excluding built-in types)

```
if else elif do while break continue return proc print println
asc chr length exit input record new null extern args
```

Eventually:

```
delete for of in get this private load save export
```

## Oddities

No semicolons.

I want `*=` (& siblings) **, but not implemented yet.**

Blocks MUST start/end with `{}`. BUT expressions don't need parens, so an
`if` statement looks like:

```
if a < 3 {
  // Even if one line
}
```

Variable declarations are optional. You just write the expression and the compiler
will deduce and enforce the type of the variable. E.g.:

```

foo: proc: int {  // note no empty list of parameters
  return 4
}

a=foo(3) // a will be of type int
```


## Comments

`//` to end of line is considered a comment.


## `for` loops

### Possible syntaxes

```
for i = 1 to 20 step 3
for i = 1 while i < 30 step 3
start i from 1 while i < 30 step 3
for i = 1 step 3 while i < 30
loop i from 20 by -1 while i < 30
make i go from 1 to 10 step 2
for i = 1 while i < 30 update i=i+1
for i = 1 until i == 30 update i=i+1
for i=0 while i < 30 do i +=1 {
```

### Actual syntax

**Winner!**

```
i=0 while i < 30 do i =i+1 {
}
```


## Procedure definition explorations

Original:

```
proc foo(a:map,b,c) returns int {
  d=a[b]+c()
  return d
}
```

Intermediate grammar:

```
foo:proc(a:map,b,c) returns int
{
  d=a[b]+c()
  return d
}
```

Now:

```
foo:proc(a:map,b,c) : int {
  d=a[b]+c()
  return d
}
```

Eventually the return type will be optional. But not implemented yet.


## Chars and strings

No "character" type - only strings, like Python. I *really* like the Python syntax for substrings.

```
"foo"[3]
"foo"[1:3]
"foo"+"bar"
"foo%d" % 3
"foo%s" % "bar"
length("foo")
```

### String length explorations

```
length("foo") // requires better built-in support but may be implementable as a quasi-unary operator
"foo".length  // also requires some built-in support
#"foo"        // pro: standard unary operator, a little built-in support. con: weird
"foo"#        // pro: ? con: weird. trailing operator
```

Winner: `length("foo")`


## Records

Definition:

```
Token: record {
  name: string
  value: int
}
```

### Usage explorations

Usage:

```
t:Token
t = new Token("Name", 3)
```

OR (much easier to parse?!)
```
t: record Token
t = record new Token("Name", 3)
// t = record Token("Name", 3)?
```

The "constructor" takes the arguments in order they were defined. Any trailing skipped arguments are set to default.

What about recursive structures? It should be OK as long as we create the symbol upon entry instead of exit, and allow nulls.

**Winner:**

```
t:Token
t = new Token
t.name = "Name"
t.value = 3
```

The "constructor" builds an empty object. Deal with it.

Also `null` is a thing.


## Arrays

Everything is like strings

```
keywords=["hi", "bye"]
keywords[0] = 'sorry' // assign
keywords[1:3] // slice, not implemented yet
print keywords[-1] // "bye" // not implemented yet
```

Length: `array.length` like Java? This will require some parsing finesse.

Winner: `length(array)`

### Declaration explorations

This actually declares and allocates the array.

```
// keywords:Array of int
// keywords:array{int}
// keywords:array[int]
// keywords:int array
// keywords:[int]
keywords:int[3] // winner because multidimensional arrays.
```

Should we allow the size at declaration time? Java does not allow this, though
C++ does. In Java all arrays are dynamically allocated. We require the size at
declaration time, except for parameters.

### Multi-dimensional (not implemented yet)

```
keywords:int[1,2]
```

### Constants

```
keywords=[1,2,3,4]
// not allowed: numbers=[1,2,functioncall(), a]
```

### Allocating

There's no separation between allocation and declaration of arrays.


## Bytes

To distinguish bytes from ints, byte hexadecimal constants use a `0y` prefix:
e.g., `0y2f`. Bytes are signed.


## Order of execution and command-line args

Statements are executed top to bottom, outside of procedures. 

Command-line arguments are provided in the `args` array (1-dimensional
array of `string`s.)
