package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.Position;

public class BreakNode extends StatementNode {

  BreakNode(Position position) {
    super(Type.BREAK, position);
  }

}
