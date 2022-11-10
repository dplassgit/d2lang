package com.plasstech.lang.d2.optimize;

import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Op;

/**
 * If there are two labels in a row, delete the 2nd and replace all gotos to the 2nd, to be gotos to
 * the 1st.
 */
class AdjacentLabelOptimizer extends LineOptimizer {

  AdjacentLabelOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  public void visit(Label firstLabel) {
    Op secondOp = getOpAt(ip() + 1);
    if (!(secondOp instanceof Label)) {
      return;
    }
    Label secondLabel = (Label) secondOp;
    // Two in a row. Delete the 2nd and replace all instances of goto(secondOp)
    deleteAt(ip() + 1);
    replaceAllMatching(
        op -> {
          if (op instanceof Goto) {
            Goto gotoOp = (Goto) op;
            return gotoOp.label().equals(secondLabel.label());
          }
          return false;
        },
        new Goto(firstLabel.label()));
  }
}
