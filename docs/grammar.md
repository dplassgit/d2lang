# Grammar


## Full Grammar

```
program -> statements main?

statements -> statement* 
statement -> assignment | print | if | while | proc | declaration | 'BREAK' | 'CONTINUE' | return_stmt | procedure_call | exit

main -> 'MAIN' mainarg? '{' statements '}'
mainarg -> '(' variable ')' // not implemented yet

assignment -> lvalue '=' expr
lvalue -> variable | variable '[' expr ']' | variable '.' variable // not fully built yet, and may become more complex

print -> 'PRINT' expr | 'PRINTLN' expr

if -> 'IF' expr '{' statements '}' elif* else?
elif -> 'ELIF' expr '{' statements '}'
else -> 'ELSE' '{' statements '}'

while -> 'WHILE' expr do? '{' statements '}'
do -> 'DO' statement

declaration -> variable ':' type | variable ':' 'PROC' procdef
type -> 'INT' | 'BOOL' | 'STRING' | type '[' expr ']' | 'RECORD' '{' declaration* '}' // RECORD not fully built yet

procdef -> params? returns? '{' statements '}'
params -> '(' param (',' param)* ')'
param -> variable (':' type)? // currently, must specify type
returns -> ':' type

return_stmt -> 'RETURN' expr?

exit -> 'EXIT' expr?

procedure_call -> variable '(' comma-separated-expressions ')'
```

## Expression grammar implemented so far

```
expr -> boolor

boolor -> booland ('|' booland)*

booland -> compare ('&' compare)*

compare -> shift (relop shift)*
relop -> '==' '!=' '>' '<' '>=' '<='

shift -> addsub ('<<' | '>>' unary)*

addsub -> muldiv (+- muldiv)*

muldiv -> unary (*/% unary)*

unary -> composite | !-+ unary | 'new' variable_name | unary_fn '(' expr ')'
unary_fn -> 'ASC' | 'CHR' | 'LENGTH'

composite -> atom ('[' expr ']')? | atom ('.' variable_name)?

atom ->   int_constant
	| boolean_constant
	| string_constant
	| variable_name
        | variable_name '(' comma-separated-expressions ')'
	| '(' expr ')'
	| '[' comma-separated-expressions ']'
	| 'INPUT'
```

Not implemented yet: power, xor

See [Java operators](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/operators.html)
for reference
