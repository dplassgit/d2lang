# Grammar


## Full Grammar

```
program -> statements main
statements -> statement* 
statement -> assignment | print | if | while | proc 
main -> 'main' { statements }
assignment -> variable '=' expr
print -> 'print' expr
if -> 'if' expr { statements } elif* else
elif -> 'elif' expr {statements}
else -> ('else' { statements })?
while -> 'while' expr do { statements }
do -> ('do' assignment)?
proc -> // TODO

```


## Comments

`//` to end of line is considered a comment.

## Node types

Node classes are:

`StatementNodes`

`abstract statementnode`

`printnode extends statementnode`

`assignmentnode extends statementnode`

`binopnode extends node`

`unaryNode extends node`

`intNode extends node`

`boolNode extends node`

`varaccessNode extends node`

`IfNode extends StatementNode` (also includes a repeated `Case`, which is like a node)

`ProcNode extends StatementNode`

`MainNode extends ProcNode`

This works.



## Expression grammar implemented so far

```
expr -> boolor

boolor -> booland (| booland)*

booland -> compare (& compare)*

compare -> addsub (relop addsub)*

relop -> == != > < >= <=

addsub -> muldiv (+- muldiv)*

muldiv -> unary (*/% unary)*

unary ->  atom | !-+ unary

atom -> int constant
	| variable name
	| boolean constant
	| '(' expr ')'
```

## Full expression grammar

Not implemented: shift

```
expr -> boolor

boolor -> booland (| booland)*

booland -> compare (& compare)*

compare -> shift (relop shift)*

relop -> == != > < >= <=

shift -> addsub (shiftop addsub)

shiftop -> << >>

addsub -> muldiv (+- muldiv)*

muldiv -> unary (*/% unary)*

unary ->  atom | !-+ unary

atom -> int constant
	| variable name
	| boolean constant
	| string constant
	| '(' expr ')'
```

See [Java operators](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/operators.html)
for reference
