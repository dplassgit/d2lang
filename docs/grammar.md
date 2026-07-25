# Grammar


## Full Grammar

```
program -> statements

statements -> statement*
statement -> assignment | print | if | while | proc | declaration | 'BREAK' | 'CONTINUE' | return_stmt | procedure_call | exit

assignment -> lvalue '=' expr | lvalue OP_EQ expr
lvalue -> variable | variable '[' expr ']' | variable '.' variable
OP_EQ -> '+=' | '-=' | '*=' | '/='

print -> 'PRINT' expr | 'PRINTLN' expr

if -> 'IF' expr '{' statements '}' elif* else?
elif -> 'ELIF' expr '{' statements '}'
else -> 'ELSE' '{' statements '}'

while -> 'WHILE' expr do? '{' statements '}'
do -> 'DO' statement

declaration -> variable ':' type | variable ':' 'EXTERN'? 'PROC' procdef
type -> 'INT' | 'BOOL' | 'STRING' | 'LONG' | 'RANGE' | type '[' expr ']' | 'RECORD' '{' declaration* '}' | 'DOUBLE' | 'BYTE' | variable

procdef -> params? returns? '{' statements '}'
params -> e | '(' (param (',' param)*)? ')'
param -> variable (':' type)?
returns -> ':' (type | 'VOID')

return_stmt -> 'RETURN' expr?

exit -> 'EXIT' expr?

procedure_call -> variable '(' comma-separated-expressions ')'

comma-separated-expressions -> expr (',' expr)*

```

## Expression grammar

```
expr -> nullcoalesce

nullcoalesce -> range ('??' range)*

range -> boolor (':' boolor)?

boolor -> boolxor (('OR' | '|') boolxor)*

boolxor -> booland (('XOR' | '^') booland)*

booland -> compare (('AND' | '&') compare)*

compare -> shift (relop shift)*
relop -> '==' | '!=' | '>' | '<' | '>=' | '<='

shift -> addsub (('<<' | '>>') addsub)*

addsub -> muldiv (('+' | '-') muldiv)*

muldiv -> unary (('*' | '/' | '%') unary)*

unary -> ('!' | '-' | '+' | 'NOT') unary |  unary_fn '(' expr ')' | 'NEW' variable | composite
unary_fn -> 'ASC' | 'CHR' | 'LENGTH'

composite -> atom (('[' expr ']') | ('.' atom))*

atom ->   int_constant
        | double_constant
        | byte_constant
        | long_constant
        | boolean_constant
        | string_constant
        | 'NULL'
        | variable
        | 'ARGS'
        | variable '(' comma-separated-expressions ')'
        | '(' expr ')'
        | '[' comma-separated-expressions ']'
        | 'INPUT'
```

## Ranges

`start:end` creates a `RANGE` from two `INT` expressions. Both endpoints are
required, and a range expression can contain only one `:`. Constant endpoints
must be non-negative and non-descending.

A range stores its endpoints at indexes `0` and `1`:

```
r = 2:5
println r[0] // 2
println r[1] // 5
```

A `RANGE` can also slice a `STRING`. The start is inclusive and the end is
exclusive, so `"hello"[1:4]` is `"ell"`. Array slicing is not implemented.

## Null coalescing

`left ?? right` evaluates to `left` when `left` is not `NULL`; otherwise, it
evaluates to `right`. The operands must have compatible nullable types, such as
`STRING`, array, or record types. `??` has the lowest precedence of the binary
operators listed above.

Not implemented yet: power (exponentiation)

See [Java operators](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/operators.html)
for reference. Note that the above does not follow the Java order of operations exactly;
the comparisons should be lower priority than equalities, and possibly boolean vs bit operations.
