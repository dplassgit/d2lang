package com.plasstech.lang.d2.optimize;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Label;

class DeadLabelOptimizer extends LineOptimizer {
  // These are the labels found in the code, and their IP.
  private final Map<String, Integer> labels = new HashMap<>();
  // These are all the labels *referenced* in the code by ifs, gotos and calls.
  private final Set<String> referencedLabels = new HashSet<>();

  DeadLabelOptimizer(int debugLevel) {
    super(debugLevel);
  }

  @Override
  protected void preProcess() {
    labels.clear();
    referencedLabels.clear();
  }

  @Override
  protected void postProcess() {
    // Post-process: all referenced labels are removed from the "labels" map.
    for (String referenced : referencedLabels) {
      labels.remove(referenced);
    }
    // Any labels left in the labels map are unreferenced. Replace with Nops.
    for (int labelIp : labels.values()) {
      deleteAt(labelIp);
    }
  }

  @Override
  public void visit(Label op) {
    // Make sure not replacing "main"
    if (op.label().equals("__main")) {
      return;
    }
    labels.put(op.label(), ip());
  }

  @Override
  public void visit(Goto op) {
    referencedLabels.add(op.label());
  }

  @Override
  public void visit(IfOp op) {
    referencedLabels.add(op.destination());
  }

  @Override
  public void visit(Call op) {
    referencedLabels.add(op.functionToCall());
  }
}
