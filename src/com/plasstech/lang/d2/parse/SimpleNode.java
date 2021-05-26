package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.Position;

/**
 * Represents a "simple" (i.e., constant or variable) node.
 */
public interface SimpleNode extends ExprNode {

  String simpleValue();
}
