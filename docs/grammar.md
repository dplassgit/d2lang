# Grammar


## Full Grammar

```
program -> statements main?

statements -> statement* 
statement -> assignment | print | if | while | proc | declaration | 'BREAK' | 'CONTINUE' | return_stmt | procedure_call | exit

main -> 'MAIN' mainarg? '{' statements '}'
mainarg -> '(' variable ')' // not implemented yet

assignment -> variable '=' expr

print -> 'PRINT' expr | 'PRINTLN' expr

if -> 'IF' expr '{' statements '}' elif* else?
elif -> 'ELIF' expr '{' statements '}'
else -> 'ELSE' '{' statements '}'

while -> 'WHILE' expr do? '{' statements '}'
do -> 'DO' assignment

declaration -> variable ':' type | variable ':' 'PROC' procdef
type -> 'INT' | 'BOOL' | 'STRING' | type '[' expr ']' | 'RECORD' '{' declaration* '}' // RECORD not fully implemented yet

procdef -> params? returns? '{' statements '}'
params -> '(' param (',' param)* ')'
param -> variable (':' type)? // currently, must specify type
returns -> ':' type

return_stmt -> 'RETURN' expr?

exit -> 'exit' expr?

procedure_call -> variable '(' comma-separated-expressions ')'
```

## Comments

`//` to end of line is considered a comment.

## Node types

Node classes are:

`interface Node`

`abstract class AbstractNode implements Node`

`BlockNode extends AbstractNode`

`interface StatementNode extends Node`

`PrintNode extends AbstractNode implements StatementNode`

`AssignmentNode extends AbstractNode implements  StatementNode`

`IfNode extends AbstractNode implements StatementNode` (also includes a repeated `Case`, which is like a node)

`DeclarationNode extends AbstractNode implements StatementNode`

`WhileNode extends AbstractNode implements StatementNode`

`ProcNode extends AbstractNode implements StatementNode`

`MainNode extends ProcNode`

`interface ExprNode extends Node` 

`BinOpNode extends AbstractNode implements ExprNode`

`UnaryNode extends AbstractNode implements ExprNode`

`ConstNode<T> extends AbstractNode implements ExprNode`

`VariableNode extends AbstractNode implements ExprNode`

This allows function/procedure calls to be both expressions (that return something) and stand-alone (either return void or throwaway the value.)

`CallNode extends AbstractNode implements ExprNode, StatementNode`


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
unary_fn -> ASC | CHR | LENGTH

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
