# Grammar


## Full Grammar

```
program -> statements

statements -> statement*
statement -> assignment | print | if | while | proc | declaration | 'BREAK' | 'CONTINUE' | return_stmt | procedure_call | exit
// reserved, but not implemented:
// FOR <variable> IN <array>
// DELETE, GET, THIS, PRIVATE, LOAD, SAVE, EXPORT

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
type -> 'INT' | 'BOOL' | 'STRING' | 'LONG' | type '[' expr ']' | 'RECORD' '{' declaration* '}' | 'DOUBLE' | 'BYTE'
// reserved, but not implemented: CHAR

procdef -> params? returns? '{' statements '}'
params -> '(' param (',' param)* ')'
param -> variable ':' type
returns -> ':' type

return_stmt -> 'RETURN' expr?

exit -> 'EXIT' expr?

procedure_call -> variable '(' comma-separated-expressions ')'
```

## Expression grammar implemented so far

```
expr -> boolor

boolor -> boolxor (('OR' | '|') boolxor)*

boolxor -> booland (('XOR' | '^') booland)*

booland -> compare (('AND' | '&') compare)*

compare -> shift (relop shift)*
relop -> '==' '!=' '>' '<' '>=' '<='

shift -> addsub (('<<' | '>>') addsub)*

addsub -> muldiv (('+' | '-') muldiv)*

muldiv -> unary (('*' | '/' | '%') unary)*

unary -> ('!' | '-' | '+' | 'NOT') unary |  unary_fn '(' expr ')' | 'NEW' variable | composite
unary_fn -> 'ASC' | 'CHR' | 'LENGTH'

composite -> atom ('[' expr ']') ('.' atom)*

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

Not implemented yet: power (exponentiation)

See [Java operators](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/operators.html)
for reference. Note that the above does not follow the Java order of operations exactly;
the comparisons should be lower priority than equalities, and possibly boolean vs bit operations.
