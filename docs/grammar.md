h# Grammar

## Implemented so far

```
statements -> stmt stmt*

stmt -> print expr | variable = expr

expr -> term (+- term)*

term -> atom (*/ atom)*

atom -> int 
	| variable 
	| '(' expr ')'
```


Node classes are:

`statementnodes`

`abstract statementnode`

`printnode extends statementnode`

`assignmentnode extends statementnode`

`binop node`

`intnode extends node`

`varaccess node extends node`


This works.

## Expression grammar

### Not implemented yet

(does not include exponent or bit operations or shift left/right)

```
expr -> booland (| booland)*

booland -> compare (& compare)*

compare -> additive (relop additive)*
relop -> == != > < <= >=

additive -> muldiv (+- muldiv)*

muldiv -> unary (*/% unary)*

unary ->  atom | !-+ atom

atom -> '(' expr ') | var | intconst | stringconst | boolconst 
```

See [Java operators](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/operators.html)
for reference
