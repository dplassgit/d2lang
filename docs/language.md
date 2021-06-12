# The D(2) Language Features

## Types & Keywords

Built-in types: int, string, boolean, (eventually: array, map, record, lambda (but not closures))

Current keywords (excluding built-in types)

```
if, else, elif, do, while, break, continue, return, proc, print, println, main
```

Eventually:

```
error, record, array(?), length, keys, values, new, delete(?), asc, chr
```

## Oddities

No semicolons because why not.

Do I want `*=` (& siblings)?

Blocks MUST start/end with `{}`. BUT expressions don't need parens, so:

```
   if a < 3 {
     // Even if one line
   }
```

## `for` loop explorations

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
} 
```

Should the return type be optional? We can infer it from the return statement and/or calls. Yes, but not implemented yet.

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

## Future thoughts:

### Records

Definition:

```
Token: record {
  name: string
  value: int
}
```

Usage:

```
t:Token
t = new Token("Name", 3)
```

The constructor takes the arguments in order they were defined.

What about recursive structures? It should be OK as long as we create the symbol upon entry instead of exit, and allow nulls.

### Arrays

Everything is like strings

```
keywords=["hi", "bye"]
keywords[0] = 'sorry' // assign
keywords[1:3] // slice
print keywords[-1] // "bye"
```

Length: `array.length` like Java. This will require some parsing finesse.

(vs length(array) like Python).

#### Declaration explorations

```
keywords:Array of int
keywords:array{int}
keywords:array[int]
keywords:int array
keywords:[int]
keywords:int[] // winner because multidimensional arrays.
```

Should we allow the size at declaration time? No. Java does not allow this, though C++ does. In Java all arrays are dynamically allocated.

#### Multi-dimensional

```
keywords:int[][]
```

#### Constants

```
keywords=[1,2,3,4]
```

#### Allocating 

```
keywords = new array[int](3)
keywords = int[3] // defaults to zeros
keywords = new int[3] // defaults to zeros -- winner, because why not
```
