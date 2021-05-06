# The D(2) Language

## Features

Built-ins: number, string, boolean, array?, map (via {}?), lambda (? - this causes havoc with scopes)

Keywords: ```if, else, elif (?), for, do, while, break, continue, return, function, new (?), delete (?), keys, values (for the map), print, main```

No semicolons because why not.

Do I want `*=` (& siblings)?

Blocks MUST start/end with `{}`. BUT expressions don't need parens, so:

```
   if a < 3 {
     // Even if one line
   }
```

What about typedefs for type safety?

## `for` loop explorations

```
for i = 1 to 20 step 3
For i = 1 while i < 30 step 3 
start i from 1 while i < 30 step 3
For i = 1 step 3 while i < 30 
loop i from 20 by -1 while i < 30
Make i go from 1 to 10 step 2
for i = 1 while i < 30 update i=i+1 
For i = 1 until i == 30 update i=i+1 
```

**Winner?**

```
for i=0 while i < 30 do i +=1 {
}
```

## Procedure definition

```
proc foo(a:map,b,c) returns number 
{
  d=a[b]+c() 
  return d
}
```
