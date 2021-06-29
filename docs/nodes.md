# I have nodes.

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
