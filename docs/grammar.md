# Grammar


## Full Grammar

```
program -> statements main?

statements -> statement* 
statement -> assignment | print | if | while | proc | declaration | 'break' | 'continue'

main -> 'main' mainarg? '{' statements '}'
mainarg -> '(' variable ')'

assignment -> variable '=' expr

print -> 'print' expr | 'println' expr

if -> 'if' expr '{' statements '}' elif* else?
elif -> 'elif' expr '{' statements '}'
else -> 'else' '{' statements '}'

while -> 'while' expr do? '{' statements '}'
do -> 'do' assignment

declaration -> variable ':' type | variable ':' 'proc' procdef
type -> 'int' | 'bool' | 'string'

procdef -> params? returns? '{' statements '}'
params -> '(' param (',' param)* ')'
param -> variable (':' type)?
returns -> 'returns' type
```

## Comments

`//` to end of line is considered a comment.

## Node types

Node classes are:

`abstract class Node`

`BlockNode extends Node`

`abstract class StatementNode extends Node`

`PrintNode extends StatementNode`

`AssignmentNode extends StatementNode`

`IfNode extends StatementNode` (also includes a repeated `Case`, which is like a node)

`DeclarationNode extends StatementNode`

`WhileNode extends StatementNode`

`ProcNode extends StatementNode`

`MainNode extends ProcNode`

`BinOpNode extends Node`

`UnaryNode extends Node`

`ConstNode<T> extends Node`

`VariableNode extends Node`


This works.



## Expression grammar implemented so far

```
expr -> boolor

boolor -> booland ('|' booland)*

booland -> compare ('&' compare)*

compare -> addsub (relop addsub)*

relop -> '==' '!=' '>' '<' '>=' '<='

addsub -> muldiv (+- muldiv)*

muldiv -> unary (*/% unary)*

unary ->  atom | !-+ unary

atom -> int constant
	| variable name
	| boolean constant
	| string constant
	| '(' expr ')'
```

Not implemented yet: shift, power, xor

See [Java operators](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/operators.html)
for reference
