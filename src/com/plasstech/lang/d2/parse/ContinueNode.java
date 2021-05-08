package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.Position;

public class ContinueNode extends StatementNode {

  ContinueNode(Position position) {
    super(Type.CONTINUE, position);
  }

}
