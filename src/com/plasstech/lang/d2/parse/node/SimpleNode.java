package com.plasstech.lang.d2.parse.node;

/**
 * Represents a "simple" (i.e., constant or variable) node.
 */
public interface SimpleNode extends ExprNode {

  String simpleValue();
}
