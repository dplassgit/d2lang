# Grammar


## Full Grammar

```
program -> statements main?

statements -> statement* 
statement -> assignment | print | if | while | proc | declaration | 'break' | 'continue' | return_stmt | procedure_call

main -> 'main' mainarg? '{' statements '}'
mainarg -> '(' variable ')' // not implemented yet

assignment -> variable '=' expr

print -> 'print' expr | 'println' expr

if -> 'if' expr '{' statements '}' elif* else?
elif -> 'elif' expr '{' statements '}'
else -> 'else' '{' statements '}'

while -> 'while' expr do? '{' statements '}'
do -> 'do' assignment

declaration -> variable ':' type | variable ':' 'proc' procdef
type -> 'int' | 'bool' | 'string' | type '[' expr ']'

procdef -> params? returns? '{' statements '}'
params -> '(' param (',' param)* ')'
param -> variable (':' type)? // currently, must specify type
returns -> ':' type

return_stmt -> 'return' expr?

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

compare -> addsub (relop addsub)*

relop -> '==' '!=' '>' '<' '>=' '<='

addsub -> muldiv (+- muldiv)*

muldiv -> unary (*/% unary)*

unary ->  array | !-+ unary

array -> atom ('[' expr ']')

atom -> int_constant
	| boolean_constant
	| string_constant
	| variable_name
  | variable_name '(' comma-separated-expressions ')'
	| '(' expr ')'
```

Not implemented yet: shift, power, xor

See [Java operators](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/operators.html)
for reference
